package com.agzr.calcProfitOnBatches.interfaces;

import com.agzr.calcProfitOnBatches.model.Product;
import java.math.BigDecimal;
import java.time.LocalDateTime;


public interface InterCompanySalesGroupingDTO {
    Product product = null;
    BigDecimal collectedQuantity = null;
    LocalDateTime intervalEnd = null;
    int intervalId = 0;
    LocalDateTime intervalStart = null;
    int intervalWeight = 0;
    String product_id = null;
}
