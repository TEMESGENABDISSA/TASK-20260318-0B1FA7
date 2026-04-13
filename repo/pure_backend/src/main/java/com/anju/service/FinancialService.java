package com.anju.service;

import com.anju.audit.AuditLogService;
import com.anju.common.BusinessException;
import com.anju.dto.InvoiceRequestCreate;
import com.anju.dto.RefundCreateRequest;
import com.anju.dto.TransactionCreateRequest;
import com.anju.entity.DailyStatement;
import com.anju.entity.FinancialRefund;
import com.anju.entity.FinancialTransaction;
import com.anju.entity.InvoiceRecord;
import com.anju.entity.InvoiceStatus;
import com.anju.entity.StatementStatus;
import com.anju.entity.SettlementRecord;
import com.anju.entity.TransactionStatus;
import com.anju.repository.DailyStatementRepository;
import com.anju.repository.FinancialRefundRepository;
import com.anju.repository.FinancialTransactionRepository;
import com.anju.repository.InvoiceRecordRepository;
import com.anju.repository.SettlementRecordRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinancialService {

    private final FinancialTransactionRepository transactionRepository;
    private final FinancialRefundRepository refundRepository;
    private final DailyStatementRepository statementRepository;
    private final InvoiceRecordRepository invoiceRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public FinancialTransaction recordTransaction(TransactionCreateRequest request) {
        validateTransactionRequest(request);
        if (transactionRepository.findByTransactionNo(request.getTransactionNo()).isPresent()) {
            throw new BusinessException("transactionNo already exists");
        }

        FinancialTransaction tx = new FinancialTransaction();
        tx.setTransactionNo(request.getTransactionNo());
        tx.setAppointmentId(request.getAppointmentId());
        tx.setChannel(request.getChannel());
        tx.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        tx.setRefundable(request.isRefundable());
        tx.setRefundAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        tx.setSettled(false);
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setOccurredAt(request.getOccurredAt() == null ? LocalDateTime.now() : request.getOccurredAt());
        FinancialTransaction saved = transactionRepository.save(tx);
        auditLogService.logAction("FINANCE_TRANSACTION_CREATE", "FinancialTransaction", saved.getTransactionNo(), null, saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public FinancialTransaction getTransaction(String transactionNo) {
        return transactionRepository.findByTransactionNo(transactionNo)
                .orElseThrow(() -> new BusinessException("transaction not found"));
    }

    @Transactional(readOnly = true)
    public List<FinancialTransaction> listTransactions(LocalDate from, LocalDate to) {
        LocalDate startDate = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate endDate = to == null ? LocalDate.now() : to;
        return transactionRepository.findByOccurredAtBetween(
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay().minusNanos(1)
        );
    }

    @Transactional
    public FinancialRefund createRefund(RefundCreateRequest request) {
        if (request.getTransactionNo() == null || request.getTransactionNo().isBlank()) {
            throw new BusinessException("transactionNo is required");
        }
        if (request.getRefundNo() == null || request.getRefundNo().isBlank()) {
            throw new BusinessException("refundNo is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("refund amount must be positive");
        }
        if (request.getRefundMode() == null) {
            throw new BusinessException("refundMode is required");
        }
        if (request.getRefundMode().name().equals("NON_ORIGINAL_ROUTE")
                && (request.getReason() == null || request.getReason().isBlank())) {
            throw new BusinessException("reason is required for non-original refund");
        }

        FinancialTransaction tx = transactionRepository.findByTransactionNo(request.getTransactionNo())
                .orElseThrow(() -> new BusinessException("transaction not found"));

        if (!tx.isRefundable()) {
            throw new BusinessException("transaction is not refundable");
        }

        BigDecimal newRefundTotal = tx.getRefundAmount().add(request.getAmount()).setScale(2, RoundingMode.HALF_UP);
        if (newRefundTotal.compareTo(tx.getAmount()) > 0) {
            throw new BusinessException("refund total cannot exceed transaction amount");
        }

        FinancialRefund refund = new FinancialRefund();
        refund.setRefundNo(request.getRefundNo());
        refund.setTransactionNo(tx.getTransactionNo());
        refund.setRefundMode(request.getRefundMode());
        refund.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        refund.setReason(request.getReason() == null ? "" : request.getReason());
        refund.setCreatedAt(LocalDateTime.now());
        refundRepository.save(refund);

        tx.setRefundAmount(newRefundTotal);
        tx.setStatus(newRefundTotal.compareTo(tx.getAmount()) == 0
                ? TransactionStatus.REFUNDED
                : TransactionStatus.PARTIAL_REFUNDED);
        transactionRepository.save(tx);
        auditLogService.logAction("FINANCE_REFUND_CREATE", "FinancialTransaction", tx.getTransactionNo(), null, refund);

        return refund;
    }

    @Transactional
    public DailyStatement generateDailyStatement(LocalDate date) {
        LocalDate targetDate = date == null ? LocalDate.now() : date;
        if (statementRepository.findByStatementDate(targetDate).isPresent()) {
            throw new BusinessException("daily statement already generated for date: " + targetDate);
        }

        LocalDateTime from = targetDate.atStartOfDay();
        LocalDateTime to = targetDate.plusDays(1).atStartOfDay().minusNanos(1);
        List<FinancialTransaction> dailyTransactions = transactionRepository.findByOccurredAtBetween(from, to);

        BigDecimal total = dailyTransactions.stream()
                .map(FinancialTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        boolean hasException = dailyTransactions.stream()
                .anyMatch(tx -> tx.getStatus() == TransactionStatus.FAILED);

        DailyStatement statement = new DailyStatement();
        statement.setStatementDate(targetDate);
        statement.setTotalAmount(total);
        statement.setTransactionCount(dailyTransactions.size());
        statement.setHasException(hasException);
        statement.setStatus(StatementStatus.GENERATED);
        statement.setCreatedAt(LocalDateTime.now());
        DailyStatement saved = statementRepository.save(statement);
        auditLogService.logAction("FINANCE_STATEMENT_GENERATE", "DailyStatement", saved.getId().toString(), null, saved);
        return saved;
    }

    @Transactional
    public InvoiceRecord createInvoiceRequest(InvoiceRequestCreate request) {
        if (request.getStatementId() == null || request.getStatementId() <= 0) {
            throw new BusinessException("statementId is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("invoice amount must be positive");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("invoice title is required");
        }
        if (request.getTaxNo() == null || request.getTaxNo().isBlank()) {
            throw new BusinessException("taxNo is required");
        }
        statementRepository.findById(request.getStatementId())
                .orElseThrow(() -> new BusinessException("statement not found"));

        InvoiceRecord invoice = new InvoiceRecord();
        invoice.setStatementId(request.getStatementId());
        invoice.setTitle(request.getTitle());
        invoice.setTaxNo(request.getTaxNo());
        invoice.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        invoice.setStatus(InvoiceStatus.REQUESTED);
        invoice.setCreatedAt(LocalDateTime.now());
        InvoiceRecord saved = invoiceRepository.save(invoice);
        auditLogService.logAction("FINANCE_INVOICE_REQUEST", "InvoiceRecord", saved.getId().toString(), null, saved);
        return saved;
    }

    @Transactional
    public InvoiceRecord markInvoiceIssued(Long invoiceId, String invoiceNo) {
        if (invoiceNo == null || invoiceNo.isBlank()) {
            throw new BusinessException("invoiceNo is required");
        }
        InvoiceRecord invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessException("invoice request not found"));
        if (invoice.getStatus() != InvoiceStatus.REQUESTED) {
            throw new BusinessException("only requested invoice can be issued");
        }
        invoice.setInvoiceNo(invoiceNo);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(LocalDateTime.now());
        InvoiceRecord saved = invoiceRepository.save(invoice);
        auditLogService.logAction("FINANCE_INVOICE_ISSUE", "InvoiceRecord", saved.getId().toString(), null, saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<DailyStatement> listStatements() {
        return statementRepository.findAll();
    }

    @Transactional(readOnly = true)
    public String exportDailyStatementCsv(Long statementId) {
        DailyStatement statement = statementRepository.findById(statementId)
                .orElseThrow(() -> new BusinessException("statement not found"));
        // Minimal CSV export that is statically verifiable and useful for offline ops.
        // Columns can be extended without breaking consumers.
        return "statementId,statementDate,totalAmount,transactionCount,hasException,status,createdAt\n"
                + statement.getId() + ","
                + statement.getStatementDate() + ","
                + statement.getTotalAmount() + ","
                + statement.getTransactionCount() + ","
                + statement.isHasException() + ","
                + statement.getStatus() + ","
                + statement.getCreatedAt()
                + "\n";
    }

    @Transactional
    public SettlementRecord createSettlement(String settlementNo, BigDecimal totalAmount) {
        if (settlementNo == null || settlementNo.isBlank()) {
            throw new BusinessException("settlementNo is required");
        }
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("totalAmount must be >= 0");
        }
        SettlementRecord s = new SettlementRecord();
        s.setSettlementNo(settlementNo);
        s.setSettlementDate(LocalDateTime.now());
        s.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        s.setStatus("CREATED");
        SettlementRecord saved = settlementRecordRepository.save(s);
        auditLogService.logAction("FINANCE_SETTLEMENT_CREATE", "SettlementRecord", saved.getSettlementNo(), null, saved);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SettlementRecord> listSettlements() {
        return settlementRecordRepository.findAll();
    }

    private void validateTransactionRequest(TransactionCreateRequest request) {
        if (request.getTransactionNo() == null || request.getTransactionNo().isBlank()) {
            throw new BusinessException("transactionNo is required");
        }
        if (request.getAppointmentId() == null || request.getAppointmentId() <= 0) {
            throw new BusinessException("appointmentId is required");
        }
        if (request.getChannel() == null) {
            throw new BusinessException("channel is required");
        }
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("amount must be positive");
        }
    }
}
