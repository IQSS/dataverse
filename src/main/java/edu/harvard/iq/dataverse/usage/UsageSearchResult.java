/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.usage;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author luopc
 */
public class UsageSearchResult {
    
    private int total;
    private int totalPages;
    private int currentPage;
    private List<Event> events;
    private List<String> histogramX;
    private List<Long> histogramY;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public List<String> getHistogramX() {
        return histogramX;
    }

    public void setHistogramX(List<String> histogramX) {
        this.histogramX = histogramX;
    }

    public List<Long> getHistogramY() {
        return histogramY;
    }

    public void setHistogramY(List<Long> histogramY) {
        this.histogramY = histogramY;
    }
    
    public List<Integer> getBrowsePages(int pageCount){
        int begin = currentPage - pageCount / 2;
        if(begin < 1) begin = 1;
        int end = begin + pageCount - 1;
        if(end > totalPages){
            end = totalPages;
            begin = end - pageCount + 1;
            if(begin < 1)begin = 1;
        }
        List<Integer> list = new ArrayList(end-begin+1);
        for(int i=begin; i<=end ;i++)list.add(i);
        return list;
    }
    
    public String getHistogramData(){
        StringBuilder str = new StringBuilder();
        str.append("[");
        for(int i=0; i<histogramX.size(); i++){
            if(i != 0)str.append(",");
            str.append("[");
            str.append("\"");
            str.append(histogramX.get(i));
            str.append("\",");
            str.append(histogramY.get(i));
            str.append("]");
        }
        str.append("]");
        return str.toString();
    }
}
