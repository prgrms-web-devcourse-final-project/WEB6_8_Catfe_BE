package com.back.global.util;

import net.ttddyy.dsproxy.QueryCount;
import net.ttddyy.dsproxy.QueryCountHolder;

public class QueryCounter {

    public static void clear() {
        QueryCountHolder.clear();
    }

    public static long getSelectCount() {
        return QueryCountHolder.getGrandTotal().getSelect();
    }

    public static long getTotalCount() {
        return QueryCountHolder.getGrandTotal().getTotal();
    }

    public static void printQueryCount() {
        QueryCount queryCount = QueryCountHolder.getGrandTotal();
        System.out.println("\n========== Query Count ==========");
        System.out.println("SELECT: " + queryCount.getSelect());
        System.out.println("INSERT: " + queryCount.getInsert());
        System.out.println("UPDATE: " + queryCount.getUpdate());
        System.out.println("DELETE: " + queryCount.getDelete());
        System.out.println("TOTAL: " + queryCount.getTotal());
        System.out.println("=================================\n");
    }
}