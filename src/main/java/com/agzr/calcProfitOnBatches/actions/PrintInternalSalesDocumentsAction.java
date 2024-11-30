package com.agzr.calcProfitOnBatches.actions;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.openxava.actions.*;
import net.sf.jasperreports.engine.*;

import org.openxava.jpa.XPersistence;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class PrintInternalSalesDocumentsAction extends JasperReportBaseAction {

    @Override
    protected JRDataSource getDataSource() throws Exception {
        ArrayList<Map<String, Object>> DocumentsMap = new ArrayList<>();
        EntityManager em = XPersistence.getManager();
        // Поиск и группировка интервалов
        String nativeQueryText = """
                    with d as (select
                    product_id,
                    row_number() over(order by product_id, intervalend, intervalstart) as row_num,
                    intervalstart as start_date,
                    intervalend as finish_date,
                    collectedquantity,
                    case when lag(product_id, 1) over (order by product_id, intervalend, intervalstart) = product_id
                    then lag(intervalend, 1) over (order by product_id, intervalend, intervalstart) else null end as prev_return_date
                from collectedinternalsalesdocumentrows
                order by row_num
                ),
                ad_islands as (
                select
                    *,
                    sum(case when d.prev_return_date >= start_date then 0 else 1 end) over(order by row_num) island_id,
                    sum(collectedquantity) over(partition by product_id) as collectedquantitysum
                from d),
                ad_islands_2 as (
                select island_id, 
                product_id, 
                (finish_date - start_date) as delta,
                min(start_date) as start_date, 
                max(finish_date) as finish_date,
                collectedquantitysum as collectedquantity
                from ad_islands as tsd
                where (finish_date - start_date) = (select  min(finish_date - start_date) as delta from ad_islands where island_id = tsd.island_id and product_id = tsd.product_id)
                group by product_id, island_id, delta, collectedquantitysum
                order by start_date,finish_date),
                ad_islands_3 as (
                select 
                    *,
                    lag(start_date, 1) over (order by finish_date, start_date)  as prev_start_date,
                    lag(finish_date, 1) over (order by finish_date, start_date)  as prev_finish_date
                from ad_islands_2),
                ad_islands_4 as (select 
                    *,
                    sum(case when (prev_start_date between start_date and finish_date) 
                    and (prev_finish_date between start_date and finish_date) then 0 else 1 end) over(order by finish_date, start_date) island_id_fin
                from ad_islands_3)
                select
                island_id_fin as island_id,
                product_id,
                max(start_date) over (partition by island_id_fin) as start_date,
                min(finish_date) over (partition by island_id_fin) as finish_date,
                sum(collectedquantity)
                from ad_islands_4
                group by island_id_fin, product_id, start_date, finish_date
                order by island_id_fin, product_id""";
        Query intervalsTable = em.createNativeQuery(nativeQueryText);
        List<Object[]> res = intervalsTable.getResultList();
        HashMap<String, Object> currentRecord = new HashMap<>();
        for (Object[] a : res) {
            System.out.printf("Doc num: %05d, product: %s, st. date: %td-%tb-%tY, fin. date:  %td-%tb-%tY, quantity: %.2f %n",
                    (BigInteger) a[0], a[1], a[2], a[2], a[2], a[3], a[3], a[3], (BigDecimal) a[4]);
            currentRecord.put("island_id", (BigInteger) a[0]);
            currentRecord.put("start_date", (Date) a[2]);
            currentRecord.put("item_name", (String) a[1]);
            DocumentsMap.add(currentRecord);
            currentRecord = new HashMap<>();
        }

        return new JRMapCollectionDataSource((Collection) DocumentsMap);
    }


    @Override
    protected String getJRXML() throws Exception {
        return "ox3_documentListReport.jrxml";
    }

    @Override
    protected Map getParameters() throws Exception {
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("ReportTitle", "Internal supply documents, 2024");
        parameters.put("ReportSubTitle", "Documents list with items");
        parameters.put("CONTEXT","/home/adrmann/eclipse_projects2024/java/calcProfitOnBatches/src/main/resources");
        return parameters;
    }

    }
