package com.agzr.calcProfitOnBatches.actions;

import com.agzr.calcProfitOnBatches.model.BatchOfGood;
import com.agzr.calcProfitOnBatches.model.Product;
import com.agzr.calcProfitOnBatches.model.SalesRecords;
import org.openxava.jpa.XPersistence;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CountSalesProfitThread implements Runnable {

    private final String productName;

    CountSalesProfitThread(String productName) {
        this.productName = productName;
    }

    @Override
    public void run() {
        EntityManager em = XPersistence.createManager();

        // Form a sequence of product batches
        Query prodQuery = em.createQuery("select p from Product p where p.name = :name");
        prodQuery.setParameter("name", productName);
        Product currentProduct = (Product) prodQuery.getSingleResult();
        Query query = em.createQuery("select b from BatchOfGood b where b.recordProduct = :pProduct and b.quantity > 0 order by b.momentOfTimePosition desc ");
        query.setParameter("pProduct", currentProduct);
        List<BatchOfGood> batchOfGoodList = query.getResultList();
        int maxBatchCount = batchOfGoodList.size();
        if (maxBatchCount == 0) {
            em.close();
            return;
        }

        Query addedCostsQuery = em.createQuery("select sum(ac.recordAmount) from BatchOfGood ac where ac.costsTargetBatch.id = :pBatchId");
        Query salesRecordQuery = em.createQuery("select sr from SalesRecords sr where sr.recordProduct = :pProduct order by sr.momentOfTimePosition asc");
        salesRecordQuery.setParameter("pProduct", currentProduct);
        List<SalesRecords> salesRecordsList = salesRecordQuery.getResultList();

        double remainedBatchItems = 0;
        double remainedBatchValue = 0;
        int batchCounter = -1;


        for (SalesRecords currentSalesRec : salesRecordsList) {
            double remainedSoldItems = currentSalesRec.getQuantity().doubleValue();
            double remainedSoldValue = currentSalesRec.getRecordAmount().doubleValue();

            ConcurrentHashMap<String, Object> tr = new ConcurrentHashMap<>();
            Optional<ConcurrentHashMap<String, Object>> totalsRecord = CountSalesProfitAction.countedProfit
                    .stream()
                    .filter(cm -> cm.containsValue(currentProduct.getName()))
                    .findFirst();

            if (totalsRecord.isPresent()) {
                tr = totalsRecord.get();
            }
            else {
                tr.put("Product", currentProduct.getName());
                tr.put("QuantitySold", 0.000);
                tr.put("AmountSold", 0.000);
                tr.put("ItemCost", 0.000);
                tr.put("Profit", 0.000);
                CountSalesProfitAction.countedProfit.add(tr);
            }


            while (remainedSoldItems > 0 && batchCounter < maxBatchCount) {

                if ((batchCounter+1) == maxBatchCount) {
                    break;
                }

                if (remainedBatchItems == 0) {
                    batchCounter++;
                    remainedBatchValue = batchOfGoodList.get(batchCounter).getRecordAmount().doubleValue();
                    remainedBatchItems = batchOfGoodList.get(batchCounter).getQuantity().doubleValue();
                    // Find additional costs of this batch, if any
                    addedCostsQuery.setParameter("pBatchId", batchOfGoodList.get(batchCounter).getId());

                    try {
                        double addedCost = ((BigDecimal) addedCostsQuery.getSingleResult()).doubleValue();
                        remainedBatchValue = remainedBatchValue + addedCost;
                        System.out.println("Found added cost for "+currentProduct.getName()+": " + addedCost);
                    } catch (Exception e) {
                        // To DO
                    }
                }

                double remainder = remainedBatchItems - remainedSoldItems;

                if (remainder == 0) {
                    tr.put("QuantitySold", (double) tr.get("QuantitySold") + remainedSoldItems);
                    tr.put("AmountSold", (double) tr.get("AmountSold") + remainedSoldValue);
                    tr.put("ItemCost", (double) tr.get("ItemCost") + remainedBatchValue);
                    tr.put("Profit", (double) tr.get("Profit") + (remainedSoldValue - remainedBatchValue));
                    remainedSoldItems = 0;
                    remainedBatchValue = 0;
                    remainedBatchItems = 0;
                }

                // It is not enough quantity in the batch remained
                if (remainder < 0) {
                    remainedSoldValue = remainedSoldValue / remainedSoldItems * (remainedSoldItems - remainedBatchItems);
                    remainedSoldItems = remainedSoldItems - remainedBatchItems;
                    tr.put("QuantitySold", (double) tr.get("QuantitySold") + remainedBatchItems);
                    tr.put("AmountSold", (double) tr.get("AmountSold") + remainedSoldValue);
                    tr.put("ItemCost", (double) tr.get("ItemCost") + remainedBatchValue);
                    tr.put("Profit", (double) tr.get("Profit") + (remainedSoldValue - remainedBatchValue));
                    remainedBatchValue = 0;
                    remainedBatchItems = 0;
                }

                // Some quantity in the batch remained
                if (remainder > 0) {
                    tr.put("QuantitySold", (double) tr.get("QuantitySold") + remainedSoldItems);
                    tr.put("AmountSold", (double) tr.get("AmountSold") + remainedSoldValue);
                    tr.put("ItemCost", (double) tr.get("ItemCost") + (remainedBatchValue / remainedBatchItems) * remainedSoldItems);
                    tr.put("Profit", (double) tr.get("Profit") + (remainedSoldValue - (remainedBatchValue / remainedBatchItems) * remainedSoldItems));
                    remainedBatchValue = remainedBatchValue - (remainedBatchValue / remainedBatchItems) * remainedSoldItems;
                    remainedBatchItems = remainedBatchItems - remainedSoldItems;
                    remainedSoldValue = 0;
                    remainedSoldItems = 0;
                }

            }

        }
        em.close();
    }
}
