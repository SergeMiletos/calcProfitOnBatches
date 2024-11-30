package com.agzr.calcProfitOnBatches.services;

import com.agzr.calcProfitOnBatches.model.*;
import net.bytebuddy.dynamic.DynamicType;
import org.hibernate.query.NativeQuery;
import org.openxava.jpa.XPersistence;
import org.openxava.model.Identifiable;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class TableFunctionsService {
    private LinkedHashMap<String, HashMap<LocalDateTime, BigDecimal>> supplyFunction;
    private LinkedHashMap<LocalDateTime, BigDecimal> salesFunction;
    private LinkedHashMap<LocalDateTime, BigDecimal> accessibleQuantityFunction;

    public void initSupplyFunction(Product product) {
        EntityManager em = XPersistence.getManager();

        em.createQuery("delete from CollectedInternalSalesDocumentRows").executeUpdate();

        Query query = em.createQuery("SELECT b FROM BatchOfGood b WHERE b.recordProduct = :product ORDER BY b.momentOfTimePosition ASC", BatchOfGood.class);
        query.setParameter("product", product);
        ArrayList<BatchOfGood> supplyList = (ArrayList<BatchOfGood>) query.getResultList();
        supplyFunction = supplyList.stream()
                .collect(Collectors
                .toMap( p -> p.getId(),
                        p -> {HashMap<LocalDateTime, BigDecimal> lm = new HashMap<>();
                                lm.put(p.getMomentOfTimePosition(), p.getQuantity());
                                return lm;},
                        (n, d) -> n,
                        LinkedHashMap::new));

    }

    public void initSupplyFunction(Product product, Company company) {
        final BigDecimal[] state = new BigDecimal[1];
        state[0] = BigDecimal.valueOf(0);

        EntityManager em = XPersistence.getManager();

        Query query = em.createQuery("SELECT b FROM BatchOfGood b WHERE b.recordProduct = :product ORDER BY b.momentOfTimePosition ASC", BatchOfGood.class);
        query.setParameter("product", product);

        ArrayList<BatchOfGood> supplyList = (ArrayList<BatchOfGood>) query.getResultList();
        supplyFunction = supplyList.stream()
                .filter(p -> ! p.getQuantity().equals(new BigDecimal(0)))
                .collect(Collectors
                .toMap( BatchOfGood::getId,
                        p -> {HashMap<LocalDateTime, BigDecimal> lm = new HashMap<>();
                              lm.put(p.getMomentOfTimePosition(), p.getQuantity());
                              return lm;},
                        (n, d) -> n,
                        LinkedHashMap::new));

        // Расчёт остатка товара нарастающим итогом только по поступлениям товара на склад
        List<Map.Entry<String, HashMap<LocalDateTime, BigDecimal>>> intList = supplyFunction.entrySet().stream()
                .peek(elem -> {
                    Optional<LocalDateTime> qDate = elem.getValue().keySet().stream().findFirst();
                    if (qDate.isPresent()) {
                        BigDecimal qLocal = elem.getValue().get(qDate.get());
                        state[0] = state[0].add(qLocal);
                        elem.getValue().put(qDate.get(), state[0]);
                    }
                }).collect(Collectors.toList());


        // Продажи по компании-получателю вычитать из поступлений не надо - остаток товаров должен быть больше на это количество,
        // ведь именно он и в этом количестве будет взят с общего склада и перепродан компании-получателю (смена владельца)
        query = em.createQuery("SELECT s FROM SalesRecords s WHERE s.recordProduct = :product AND s.recordCompany <> :company ORDER BY s.momentOfTimePosition ASC", SalesRecords.class);
        query.setParameter("company", company);
        query.setParameter("product", product);
        ArrayList<SalesRecords> salesList = (ArrayList<SalesRecords>) query.getResultList();

        // Уменьшим остаток на складе на количество в реализациях
        for (SalesRecords elem : salesList) {
            List<Map.Entry<String, HashMap<LocalDateTime, BigDecimal>>> res = supplyFunction.entrySet()
                    .stream()
                    .filter(entry -> entry.getValue()
                                          .keySet()
                                          .stream()
                                          .findFirst().get()
                                          .compareTo(elem.getMomentOfTimePosition()) <= 0)
                    .collect(Collectors.toList());
            if ( !res.isEmpty() ) {
                BigDecimal intermVal = res.get(res.size()-1).getValue().entrySet().stream().findFirst().get().getValue();
                res.get(res.size()-1).getValue().entrySet().stream()
                        .findFirst().get()
                        .setValue(intermVal.subtract(elem.getQuantity()));
            }
        }

        // Находим интервал доступного остатка, в который попадает очередная продажа по компании
        // и, обработав её, корректируем доступный остаток товара (уменьшаем его на реализованное количество)
        query = em.createQuery("SELECT s FROM SalesRecords s WHERE s.recordProduct = :product AND s.recordCompany = :company ORDER BY s.momentOfTimePosition ASC", SalesRecords.class);
        query.setParameter("company", company);
        query.setParameter("product", product);
        salesList = (ArrayList<SalesRecords>) query.getResultList();

        for (SalesRecords elem: salesList) {
            state[0] = elem.getQuantity();

            // Найдём минимальную дату, в которой ещё достаточно остатка товара для перепродажи
            Optional<Map.Entry<String, HashMap<LocalDateTime, BigDecimal>>> iqEntry = supplyFunction.entrySet().stream()
                    .filter(sElem -> (sElem.getValue().entrySet().stream().findFirst().get().getKey().isBefore(elem.getMomentOfTimePosition())
                            || sElem.getValue().entrySet().stream().findFirst().get().getKey().isEqual(elem.getMomentOfTimePosition()))
                            && sElem.getValue().entrySet().stream().findFirst().get().getValue().compareTo(elem.getQuantity()) >= 0)
                    .findFirst();

            if (iqEntry.isPresent()) {
                Optional<Map.Entry<LocalDateTime, BigDecimal>> quantityEntry = iqEntry.get().getValue().entrySet().stream().findFirst();
                if (quantityEntry.isPresent()) {

                    CollectedInternalSalesDocumentRows cr = new CollectedInternalSalesDocumentRows();
                    cr.setIntervalStart(quantityEntry.get().getKey());
                    cr.setIntervalEnd(elem.getMomentOfTimePosition());
                    cr.setProduct(product);
                    cr.setCollectedQuantity(quantityEntry.get().getValue());
                    em.persist(cr);

                    state[0] = quantityEntry.get().getValue().subtract(state[0]);
                    quantityEntry.get().setValue(state[0]);
                }
            }
        }

    }

    public BigDecimal getQuantity(LocalDateTime dt) {
        Optional<HashMap<LocalDateTime, BigDecimal>> res = supplyFunction.entrySet().stream()
                .filter(entry -> entry.getValue().entrySet().stream().findFirst().get().getKey().isBefore(dt)
                        || entry.getValue().entrySet().stream().findFirst().get().getKey().isEqual(dt))
                .reduce((first, second) -> second).map(Map.Entry::getValue);
        res.ifPresent(System.out::println);
        return res.get().entrySet().stream().findFirst().get().getValue();
    }

/*    public void subtractQuantity(LocalDateTime dt, BigDecimal v) {
        Optional<Map.Entry<LocalDateTime, BigDecimal>> res = supplyFunction.entrySet().stream()
                .filter(entry -> entry.getKey().isBefore(dt) || entry.getKey().isEqual(dt))
                .reduce((first, second) -> second);
        if (res.isPresent()) {
            Map.Entry<LocalDateTime, BigDecimal> mapEntry = res.get();
            mapEntry.setValue(mapEntry.getValue().subtract(v));
        }
    }
*/

/*
    public BigDecimal getAccessibleQuantity(LocalDateTime dt) {
        BigDecimal result = new BigDecimal(0);
        if (accessibleQuantityFunction != null) {
            accessibleQuantityFunction.clear();
        }
        else {
            accessibleQuantityFunction = new LinkedHashMap<LocalDateTime, BigDecimal>();
        }

        accessibleQuantityFunction.putAll(supplyFunction);

        final BigDecimal[] intermedVal = new BigDecimal[1];
        intermedVal[0] = BigDecimal.ZERO;
        Map.Entry<LocalDateTime, BigDecimal> nl = accessibleQuantityFunction.entrySet().stream()
                .peek(entry -> {intermedVal[0] = intermedVal[0].add(entry.getValue());
                            System.out.println("-- 1. " + entry.getValue().toString());
                            entry.setValue(intermedVal[0]);
                            System.out.println("-- 2. " + entry.getValue().toString() + ", dt " + entry.getKey().toString() + " (intermed = " + intermedVal[0].toString() + ")");
                        }
                ).max((localDateTimeBigDecimalEntry, t1) -> localDateTimeBigDecimalEntry.getKey().compareTo(t1.getKey())).get();
        ;

        // Subtract sales records,
        return result;
    }

    public void addQuantity(LocalDateTime dt, BigDecimal v) {
        Optional<Map.Entry<LocalDateTime, BigDecimal>> res = supplyFunction.entrySet().stream()
                .filter(entry -> entry.getKey().isBefore(dt) || entry.getKey().isEqual(dt))
                .reduce((first, second) -> second);
        if (res.isPresent()) {
            Map.Entry<LocalDateTime, BigDecimal> mapEntry = res.get();
            mapEntry.setValue(mapEntry.getValue().add(v));
        }
    }
*/
}
