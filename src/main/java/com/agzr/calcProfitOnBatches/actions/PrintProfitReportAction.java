package com.agzr.calcProfitOnBatches.actions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.agzr.calcProfitOnBatches.model.BatchOfGood;
import com.agzr.calcProfitOnBatches.model.Company;

import com.agzr.calcProfitOnBatches.services.TableFunctionsService;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.openxava.actions.*;
import net.sf.jasperreports.engine.*;

import org.openxava.jpa.XPersistence;

import javax.persistence.*;


public class PrintProfitReportAction extends JasperReportBaseAction {


    @Override
    protected JRDataSource getDataSource() throws Exception {
        CountSalesProfitAction ca = new CountSalesProfitAction();
        ArrayList<Map<String, Object>> profitMap = new ArrayList<>();

        List<ConcurrentHashMap<String, Object>> cm = ca.getProfitMap();
        for (ConcurrentHashMap<String, Object> currRecord : cm) {
            HashMap<String, Object> newMap = new HashMap<>(currRecord);
            profitMap.add(newMap);
        }


        EntityManager em = XPersistence.getManager();
        em.createQuery("delete from CollectedInternalSalesDocumentRows").executeUpdate();

        TypedQuery<Company> tQuery = em.createQuery("SELECT c FROM Company c WHERE c.name LIKE :name", Company.class);
        tQuery.setParameter("name", "alyansstrojkompleks  ooo");
        Company fc = (Company) tQuery.getSingleResult();

        int productCounter = 0;
        List<List<Object[]>> licDTO = new ArrayList<>();

        for (Map<String, Object> stringObjectMap : profitMap) {
            Query query = em.createQuery("SELECT b FROM BatchOfGood b WHERE b.recordProduct.name LIKE :ptName ORDER BY b.momentOfTimePosition ASC", BatchOfGood.class);
            query.setParameter("ptName", stringObjectMap.get("Product"));

            ArrayList<BatchOfGood> productBatch = (ArrayList<BatchOfGood>) query.getResultList();
            if ( !productBatch.isEmpty() ) {
                TableFunctionsService ts = new TableFunctionsService();

                ts.initSupplyFunction(productBatch.get(productBatch.size() - 1).getRecordProduct(), fc);
                BigDecimal res = ts.getQuantity(LocalDateTime.now());


                query = em.createNativeQuery("select * from CollectedInternalSalesDocumentRows cdr " +
                        "where cdr.product_id = :ptProduct", "queryResultComplex");
                query.setParameter("ptProduct", productBatch.get(productBatch.size() - 1).getRecordProduct().getId());
                List<Object[]> qResult = query.getResultList();

                if ( !qResult.isEmpty() ) {
                    licDTO.add(qResult);

                    productCounter++;

                    if (productCounter > 25)
                        break;
                }
            }
        }

        return new JRMapCollectionDataSource((Collection) profitMap);
    }

    @Override
    protected String getJRXML() throws Exception {
        return "ox3_profitReport.jrxml";
    }

    @Override
    protected Map getParameters() throws Exception {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("ReportTitle", "Profit report test, 2024");

        return parameters;
    }
}