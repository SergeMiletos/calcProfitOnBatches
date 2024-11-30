package com.agzr.calcProfitOnBatches.actions;

import com.agzr.calcProfitOnBatches.model.BatchOfGood;
import com.agzr.calcProfitOnBatches.model.Company;
import com.agzr.calcProfitOnBatches.model.Product;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openxava.actions.ViewBaseAction;
import org.openxava.jpa.XPersistence;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoadBatchesData extends ViewBaseAction {

    public void execute() throws Exception {
        Product batchProduct;
        Company batchCompany;
        BatchOfGood batchRecord;
        String fileName = "test.xml";
        EntityManager em = XPersistence.getManager();
        ObjectMapper mapper = new ObjectMapper();

        InputStream ioStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(fileName);

        JsonNode rootNode = mapper.readTree(ioStream);


        for (JsonNode batchNode : rootNode) {
            batchRecord = new BatchOfGood();
            batchRecord.setMomentOfTimePosition(LocalDateTime.parse(batchNode.get("momentOfTimePosition").asText()));
            Query pquery = em.createQuery("SELECT p FROM Product p WHERE p.name = :name");
            pquery.setParameter("name", batchNode.get("productName").asText());
            pquery.setFirstResult(0);
            pquery.setMaxResults(1);

            try {
                batchProduct = (Product) pquery.getSingleResult();
            }
            catch(NoResultException e){
                batchProduct = new Product();
                batchProduct.setName(batchNode.get("productName").asText());
                batchProduct.setDescription(batchNode.get("productName").asText());
                batchProduct.setEom("pcs");
                em.persist(batchProduct);
            }

            if (em.contains(batchProduct)) {
                batchRecord.setRecordProduct(batchProduct);
            } else
                continue;

            pquery = em.createQuery("SELECT c FROM Company c WHERE c.name = :name");
            pquery.setParameter("name", batchNode.get("companyName").asText());
            pquery.setFirstResult(0);
            pquery.setMaxResults(1);

            try {
                batchCompany = (Company) pquery.getSingleResult();
            }
            catch(NoResultException e){
                if (!batchNode.get("companyName").asText().trim().isEmpty()) {
                    batchCompany = new Company();
                    batchCompany.setName(batchNode.get("companyName").asText());
                    batchCompany.setDescription(batchNode.get("companyName").asText());
                    em.persist(batchCompany);

                } else
                    continue;
            }

            if (em.contains(batchCompany)) {
                batchRecord.setRecordCompany(batchCompany);
            } else
                continue;


            batchRecord.setSupplyDocumentId(batchNode.get("costsTargetBatch").asText());
            batchRecord.setRecordDocumentId(batchNode.get("supplyDocumentId").asText());

            if (batchRecord.getSupplyDocumentId().compareTo(batchRecord.getRecordDocumentId()) != 0) {
                pquery = em.createQuery("SELECT b FROM BatchOfGood b WHERE b.recordDocumentId = :docId ORDER BY b.momentOfTimePosition ASC");
                pquery.setParameter("docId", batchNode.get("costsTargetBatch").asText());
                pquery.setFirstResult(0);
                pquery.setMaxResults(1);

                try {
                    batchRecord.setCostsTargetBatch((BatchOfGood) pquery.getSingleResult());
                }
                catch (NoResultException e) {
                    System.out.println(" No such document id: "+batchNode.get("costsTargetBatch").asText()+". Exception: "+e.getMessage());
                    continue;
                }
            }
            batchRecord.setQuantity(BigDecimal.valueOf(batchNode.get("quantity").asDouble()));
            batchRecord.setRecordAmount(BigDecimal.valueOf(batchNode.get("recordAmount").asDouble()));
            em.persist(batchRecord);
        }
        XPersistence.commit();

    }
}
