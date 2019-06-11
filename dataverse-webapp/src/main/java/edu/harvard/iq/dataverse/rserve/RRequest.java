/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.harvard.iq.dataverse.rserve;

import java.util.logging.Logger;
import org.rosuda.REngine.Rserve.*;
import org.rosuda.REngine.Rserve.RserveException;
import org.rosuda.REngine.REXP;

/**
 * original
 * @author Matt Owen
 * @author Leonid Andreev
 */

public class RRequest {
  
  private static final Logger LOG = Logger.getLogger(RRequest.class.getPackage().getName());

  private RConnection mRC;
  private String mHost, mUser, mPassword, mScript;
  private int mPort;

  /*
   * Construct a Request to the R Server
   * @param host a string specifying the host URL
   * @param port an integer specifying the port
   * @param script a string representing the entire script to be executed
   */
  public RRequest (String host, int port, String user, String pass, String script) {
    
    // Login info
    mHost = host;
    mPort = port;
    mUser = user;
    mPassword = pass;
    
    // Script info
    mScript = script;
  }
  /*
   * Set the value of the Script
   */
  public RRequest script (String script) {
    mScript = script;
    return this;
  }
  /**
   * Evaluate script
   * @return R-Expression
   */
  public REXP eval () {
    REXP result = null;
    
    try {
      open();
      
      result = mRC.eval(mScript);
    }
    catch (RserveException e) {
      LOG.warning(String.format("RRequest: %s", e.getMessage()));
    }
    finally {
      close();
    }
    
    return result;
  }
  /*
   * Return a String Representing the Object
   * @return a string... representing the object
   */
  @Override
  public String toString () {
    return "Host: " + mHost + "\nPort: " + String.valueOf(mPort) + "\n";
  }
  /*
   * Open the R Connection
   */
  private void open () {
    LOG.fine(String.format("RRequest: Attempting connection to RSERVE %s on port %d", mHost, mPort));

    try {
      // Attempt connection
      mRC = new RConnection(mHost, mPort);
      
      // Attempt login
      mRC.login(mUser, mPassword);
      
      // SERVER VERSION
      LOG.fine("SERVER VERSION = " + mRC.getServerVersion());
      
      // Output everything is cool message
      LOG.fine(String.format("RRequest: Successful Connection to RSERVE on %s %d", mHost, mPort));
    }
    catch (RserveException exc) {
      mRC = null;
      int code = exc.getRequestReturnCode();
      
      // If bad hostname *OR* bad port
      if (code == -1)
        LOG.fine("RRequest: Connection refused because of bad HOSTNAME or PORT");
      
      // If bad username *OR* bad password
      if (code == 65)
        LOG.fine("RRequest: Connection refused because of bad USERNAME or PASSWORD");
   
      // Output warning message
      LOG.warning(String.format("RRequest: Failed Connection to RSERVE on %s %d", mHost, mPort));
      
      // Stack trace...
      exc.printStackTrace();
    }
  }
  /*
   * Close the R Connection
   */
  private void close () {
    mRC.close();
  }
  /*
   * Get R Connection
   */
  public RConnection getRConnection () {
    return mRC;
  }
}