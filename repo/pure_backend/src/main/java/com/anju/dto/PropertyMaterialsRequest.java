package com.anju.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyMaterialsRequest {
    private List<Long> fileIds;
}
