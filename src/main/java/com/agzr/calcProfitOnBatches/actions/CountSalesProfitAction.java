package com.agzr.calcProfitOnBatches.actions;

import com.agzr.calcProfitOnBatches.model.Product;
import org.openxava.actions.ViewBaseAction;
import org.openxava.jpa.XPersistence;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.concurrent.*;

public class CountSalesProfitAction extends ViewBaseAction {
    private volatile List<Product> products;
    static CopyOnWriteArrayList<ConcurrentHashMap<String, Object>> countedProfit;

    @Override
    public void execute() throws Exception {
        countedProfit = new CopyOnWriteArrayList<>();

        EntityManager em = XPersistence.getManager();

        // Get list of sold items
        Query query = em.createQuery("SELECT DISTINCT s.recordProduct FROM SalesRecords s ORDER BY s.recordProduct.name", Product.class);

        products = (List<Product>) query.getResultList();

        boolean finished;
        ExecutorService es = Executors.newCachedThreadPool();
        for (Product product : products) {
            CountSalesProfitThread runNowCsp = new CountSalesProfitThread(product.getName());
            es.execute(runNowCsp);
        }
        es.shutdown();
        finished = es.awaitTermination(3, TimeUnit.MINUTES);

        System.out.println("Finished. (Normal shutdown: "+finished+")");

    }

    public List<ConcurrentHashMap<String, Object>> getProfitMap() throws Exception {
        execute();
        return countedProfit;
    }

}
