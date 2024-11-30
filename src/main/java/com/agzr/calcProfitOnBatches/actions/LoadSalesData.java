package com.agzr.calcProfitOnBatches.actions;

import com.agzr.calcProfitOnBatches.model.Company;
import com.agzr.calcProfitOnBatches.model.SalesRecords;
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

public class LoadSalesData extends ViewBaseAction {

    public void execute() throws Exception {
        Product salesProduct;
        Company salesCompany;
        SalesRecords salesRecord;

        String fileName = "test_sales.xml";
        EntityManager em = XPersistence.getManager();
        ObjectMapper mapper = new ObjectMapper();

        InputStream ioStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream(fileName);

        JsonNode rootNode = mapper.readTree(ioStream);


        for (JsonNode batchNode : rootNode) {
            salesRecord = new SalesRecords();
            salesRecord.setMomentOfTimePosition(LocalDateTime.parse(batchNode.get("momentOfTimePosition").asText()));
            Query pquery = em.createQuery("SELECT p FROM Product p WHERE p.name = :name");
            pquery.setParameter("name", batchNode.get("productName").asText());


            try {
                salesProduct = (Product) pquery.getSingleResult();
            }
            catch(NoResultException e){
                salesProduct = new Product();
                salesProduct.setName(batchNode.get("productName").asText());
                salesProduct.setDescription(batchNode.get("productName").asText());
                salesProduct.setEom("pcs");
                em.persist(salesProduct);
            }

            if (em.contains(salesProduct)) {
                salesRecord.setRecordProduct(salesProduct);
            }
            pquery = em.createQuery("SELECT c FROM Company c WHERE c.name = :name");
            pquery.setParameter("name", batchNode.get("companyName").asText());


            try {
                salesCompany = (Company) pquery.getSingleResult();
            }
            catch(NoResultException e){
                salesCompany = new Company();
                salesCompany.setName(batchNode.get("companyName").asText());
                salesCompany.setDescription(batchNode.get("companyName").asText());
                em.persist(salesCompany);
            }

            if (em.contains(salesCompany)) {
                salesRecord.setRecordCompany(salesCompany);
            }

            salesRecord.setSalesDocumentId(batchNode.get("salesDocumentId").asText());

            salesRecord.setQuantity(BigDecimal.valueOf(batchNode.get("quantity").asDouble()));
            salesRecord.setRecordAmount(BigDecimal.valueOf(batchNode.get("recordAmount").asDouble()));
            em.persist(salesRecord);

        }
        XPersistence.commit();

    }
}
