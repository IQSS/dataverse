package edu.harvard.iq.dataverse.globus;

import java.util.ArrayList;

public class Transferlist {


    private ArrayList<SuccessfulTransfer> DATA;

    public void setDATA(ArrayList<SuccessfulTransfer> DATA) {
        this.DATA = DATA;
    }

    public ArrayList<SuccessfulTransfer> getDATA() {
        return DATA;
    }

}
