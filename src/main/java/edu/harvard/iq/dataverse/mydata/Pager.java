/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse.mydata;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.ServletContextTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import static edu.harvard.iq.dataverse.DatasetFieldType.FieldType.URL;
import java.io.File;

import java.io.IOException;
import java.io.InputStreamReader;
import static java.lang.Math.max;
import static java.lang.Math.min;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletContext;
import static org.atmosphere.di.ServletContextHolder.getServletContext;

/**
 *
 * @author rmp553
 */
public class Pager {
    
    /* inputs */
    public int numResults;
    public int docsPerPage = 10;
    public int selectedPageNumber = 1;

    /* calculated */
    public int pageCount = 0;
    public int[] pageNumberList = null;

    public int previousPageNumber = 0;
    public int nextPageNumber = 0;
    
    public int startCardNumber = 0;
    public int endCardNumber = 0;
    
    
    public Pager(int numResults, int docsPerPage, int selectedPageNumber) {
        
        if (numResults < 0){
            throw new IllegalArgumentException("numResults must be 0 or higher");
        }
        if (docsPerPage < 1){
            throw new IllegalArgumentException("docsPerPage must be 1 or higher");
        }
        if (selectedPageNumber < 1){
            throw new IllegalArgumentException("selectedPageNumber must be 1 or higher");
        }      
        this.numResults = numResults;
        this.docsPerPage = docsPerPage;
        this.selectedPageNumber = selectedPageNumber;
        makePageStats();
    }

    private void makePageStats(){
        
        if (numResults == 0){
            this.selectedPageNumber = 0;
            return;
        }
        
       // page count
        this.pageCount = numResults / docsPerPage;
        if ((this.numResults % this.docsPerPage) > 0){
            this.pageCount += 1;
        }
    
        // Sanity check for the selected page
        if (this.selectedPageNumber > this.pageCount){
            this.selectedPageNumber = 1;
        }
    
        // page number list
        pageNumberList = new int[this.pageCount];
        for(int i=0; i<this.pageCount; i++){
            pageNumberList[i] = i + 1;
         }

        // prev/next page numbers
        this.previousPageNumber =  max(this.selectedPageNumber-1, 1); // must be at least 1
        this.nextPageNumber =  min(this.selectedPageNumber+1, this.pageCount); // must be at least 1
        this.nextPageNumber = max(this.nextPageNumber, 1);
        
        // start/end card numbers
        this.startCardNumber =  (this.docsPerPage * (this.selectedPageNumber - 1)) + 1;
        if (this.numResults == 0){
            this.endCardNumber = 0;
        }else{
            this.endCardNumber = min(this.startCardNumber + (this.docsPerPage-1), this.numResults );
        }
         

    }
    
    public boolean isPagerNecessary(){
        
        if (this.pageCount > 1){
            return true;
        }
        return false;
    }
    
    public boolean hasPreviousPageNumber(){
        
        return this.selectedPageNumber > 1;
    }
    
    public boolean hasNextPageNumber(){
        if (this.pageCount > 1){
            if (selectedPageNumber < this.pageCount){
                return true;
            }
        }
        return false;
    }
    
    
    /**
     * get numResults
     * @return 
     */
    public int getNumResults(){
        return this.numResults;
    }
    

    /**
     * @param numResults
     */
    public void setNumResults(int numResults){
        this.numResults = numResults;
    }
    

    /**
     * get docsPerPage
     * @return 
     */
    public int getDocsPerPage(){
        return this.docsPerPage;
    }
    

    /**
     * @param docsPerPage
     */
    public void setDocsPerPage(int docsPerPage){
        this.docsPerPage = docsPerPage;
    }
    

    /**
     * get selectedPageNumber
     * @return 
     */
    public int getSelectedPageNumber(){
        return this.selectedPageNumber;
    }
    

    /**
     * @param selectedPageNumber
     */
    public void setSelectedPageNumber(int selectedPageNumber){
        this.selectedPageNumber = selectedPageNumber;
    }
    

    /**
     * get pageCount
     * @return 
     */
    public int getPageCount(){
        return this.pageCount;
    }
    

    /**
     * @param pageCount
     */
    public void setPageCount(int pageCount){
        this.pageCount = pageCount;
    }
    
    /**
     * get getPageNumberListAsStringList
     * @return 
     */
    public List<String> getPageNumberListAsStringList(){
        List<String> newList = new ArrayList<String>(pageNumberList.length);
        for (int pgNum : pageNumberList) { 
          newList.add(String.valueOf(pgNum)); 
        }
        return newList;
    }

    /**
     * get pageNumberList
     * @return 
     */
    public int[] getPageNumberList(){
        return this.pageNumberList;
    }
    

    /**
     * @param pageNumberList
     */
    public void setPageNumberList(int[] pageNumberList){
        this.pageNumberList = pageNumberList;
    }
    

    /**
     * get previousPageNumber
     * @return 
     */
    public int getPreviousPageNumber(){
        return this.previousPageNumber;
    }
    

    /**
     * @param previousPageNumber
     */
    public void setPreviousPageNumber(int previousPageNumber){
        this.previousPageNumber = previousPageNumber;
    }
    

    /**
     * get nextPageNumber
     * @return 
     */
    public int getNextPageNumber(){
        return this.nextPageNumber;
    }
    

    /**
     * @param nextPageNumber
     */
    public void setNextPageNumber(int nextPageNumber){
        this.nextPageNumber = nextPageNumber;
    }
    

    /**
     * get startCardNumber
     * @return 
     */
    public int getStartCardNumber(){
        return this.startCardNumber;
    }
    

    /**
     * @param startCardNumber
     */
    public void setStartCardNumber(int startCardNumber){
        this.startCardNumber = startCardNumber;
    }
    

    /**
     * get endCardNumber
     * @return 
     */
    public int getEndCardNumber(){
        return this.endCardNumber;
    }
    

    /**
     * @param endCardNumber
     */
    public void setEndCardNumber(int endCardNumber){
        this.endCardNumber = endCardNumber;
    }

    public void showClasspaths(){
        ClassLoader cl = ClassLoader.getSystemClassLoader();
 
        URL[] urls = ((URLClassLoader)cl).getURLs();
 
        for(URL url: urls){
        	System.out.println(url.getFile());
        }
    }
    
    public static void main(String[] args) throws IOException {
        /*
        Handlebars handlebars = new Handlebars();

        Template template = handlebars.compileInline("Hello {{this}}!");

        System.out.println(template.apply("Handlebars.java"));
       */
        Pager pager = new Pager(100, 10, 1);
        //pager.showClasspaths();
        //Handlebars handlebars = new Handlebars();
    //final File f = new File(Pager.class.getProtectionDomain().getCodeSource().getLocation().getPath());
    //System.out.println("path: " + f.getAbsolutePath());
        
        /* 
        // servlet context
        ServletContext servletContext = getServletContext();
        TemplateLoader loader = new ServletContextTemplateLoader(servletContext,
  "/WEB-INF/pages", ".html");
        Handlebars handlebars = new Handlebars(loader);
        Template template = handlebars.compile("hello"); 
        System.out.println(template.apply("there"));
                */

        /*
        MustacheFactory mf = new DefaultMustacheFactory(); 
        Mustache fromFile mf.compile(new InputStreamReader(getServletContext().getResourceAsStream("mytemplate")), "mytemplate");
        */
        
        // Template
        //TemplateLoader loader = new ClassPathTemplateLoader();
        TemplateLoader loader = new FileTemplateLoader("/Users/rmp553/NetBeansProjects/dataverse/src/main/java/edu/harvard/iq/dataverse/mydata/",
  ".hbs");
        //loader.setPrefix("resources/");
        //loader.setSuffix(".html");
        Handlebars handlebars = new Handlebars(loader);
        
        Template template = handlebars.compile("mytemplate");

        
        Map<String, Object> dict = new HashMap<String, Object>();
        //dict.put("array", new String[]{"s1", "s2" });
        dict.put("numResults", pager.numResults);
        dict.put("pager", pager);
        //dict.put("pager", pager);
        //new String[]{"s1", "s2" });
        //assertEquals("s1", template.apply(context));
        String pageString = template.apply(dict);
        
        pager.msgt("pageString: " + pageString);
        ///System.out.println(template.apply(pager.numResults));
        
        }
    
    private void msg(String s){
        System.out.println(s);
    }
    
    private void msgt(String s){
        msg("-------------------------------");
        msg(s);
        msg("-------------------------------");
    }
} 