package com.carddemo.transaction.dto;

import com.carddemo.transaction.entity.BillPayee;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillPayeeResponse {
    private Long payeeId;
    private Integer customerId;
    private String payeeName;
    private String payeeType;
    private String payeeTypeName;
    private String payeeAccountNumber;
    private String nickname;
    private Boolean isActive;

    public static BillPayeeResponse fromEntity(BillPayee entity) {
        return BillPayeeResponse.builder()
                .payeeId(entity.getPayeeId())
                .customerId(entity.getCustomerId())
                .payeeName(entity.getPayeeName())
                .payeeType(entity.getPayeeType())
                .payeeTypeName(entity.getPayeeTypeName())
                .payeeAccountNumber(entity.getPayeeAccountNumber())
                .nickname(entity.getNickname())
                .isActive(entity.getIsActive())
                .build();
    }
}
