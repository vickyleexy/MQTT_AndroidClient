/*
 * Licensed Materials - Property of IBM
 *
 * 5747-SM3
 *
 * (C) Copyright IBM Corp. 1999, 2012 All Rights Reserved.
 *
 * US Government Users Restricted Rights - Use, duplication or
 * disclosure restricted by GSA ADP Schedule Contract with
 * IBM Corp.
 *
 */
package com.ibm.msg.android;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.LogManager;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.ibm.msg.android.ActionListener.Action;
import com.ibm.msg.android.Connection.ConnectionStatus;
import com.ibm.msg.android.service.MqttClientAndroidService;

/**
 * Deals with actions performed in the {@link ClientConnections} activity
 * and the {@link ConnectionDetails} activity and associated fragments
 *
 */
public class Listener implements OnMenuItemClickListener {

  /** The handle to a {@link Connection} object which contains the {@link MqttClientAndroidService} associated with this object **/
  private String clientHandle = null;

  /** {@link ConnectionDetails} reference used to perform some actions**/
  private ConnectionDetails connectionDetails = null;
  /** {@link ClientConnections} reference used to perform some actions**/
  private ClientConnections clientConnections = null;
  /** {@link Context} used to load and format strings **/
  private Context context = null;

  static boolean logging = false;

  /**
   * Constructs a listener object for use with {@link ConnectionDetails} activity and
   * associated fragments.
   * @param connectionDetails The instance of {@link ConnectionDetails}
   * @param clientHandle The handle to the client that the actions are to be performed on
   */
  public Listener(ConnectionDetails connectionDetails, String clientHandle)
  {
    this.connectionDetails = connectionDetails;
    this.clientHandle = clientHandle;
    context = connectionDetails;

  }

  /**
   * Constructs a listener object for use with {@link ClientConnections} activity.
   * @param clientConnections The instance of {@link ClientConnections}
   */
  public Listener(ClientConnections clientConnections) {
    this.clientConnections = clientConnections;
    context = clientConnections;
  }

  /**
   * Perform the needed action required based on the button that
   * the user has clicked.
   * 
   * @param item The menu item that was clicked
   * @return If there is anymore processing to be done
   * 
   */
  @Override
  public boolean onMenuItemClick(MenuItem item) {

    int id = item.getItemId();

    switch (id)
    {
      case R.id.publish :
        publish();
        break;
      case R.id.subscribe :
        subscribe();
        break;
      case R.id.newConnection :
        createAndConnect();
        break;
      case R.id.disconnect :
        disconnect();
        break;
      case R.id.connectMenuOption :
        reconnect();
        break;
      case R.id.startLogging :
        enablePahoLogging();
        break;
      case R.id.endLogging :
        disablePahoLogging();
        break;
      case R.id.dumpLog :
        writeLog();
        break;
    }

    return false;
  }

  /**
   * Reconnect the selected client
   */
  private void reconnect() {

    Connections.getInstance().getConnection(clientHandle).changeConnectionStatus(ConnectionStatus.CONNECTING);

    Connection c = Connections.getInstance().getConnection(clientHandle);
    try {
      c.getClient().connect(c.getConnectionOptions(), null, new ActionListener(context, Action.CONNECT, clientHandle, null));
    }
    catch (MqttSecurityException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to reconnect the client with the handle " + clientHandle, e);
      c.addAction("Client failed to connect");
    }
    catch (MqttException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to reconnect the client with the handle " + clientHandle, e);
      c.addAction("Client failed to connect");
    }

  }

  /**
   * Disconnect the client
   */
  private void disconnect() {

    Connection c = Connections.getInstance().getConnection(clientHandle);
    try {
      c.getClient().disconnect(null, new ActionListener(context, Action.DISCONNECT, clientHandle, null));
    }
    catch (MqttException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to disconnect the client with the handle " + clientHandle, e);
      c.addAction("Client failed to disconnect");
    }

  }

  /**
   * Subscribe to a topic that the user has specified
   */
  private void subscribe()
  {
    String topic = ((EditText) connectionDetails.findViewById(R.id.topic)).getText().toString();
    ((EditText) connectionDetails.findViewById(R.id.topic)).getText().clear();

    try {
      String[] topics = new String[1];
      topics[0] = topic;
      Connections.getInstance().getConnection(clientHandle).getClient()
          .subscribe(topic, 0, null, new ActionListener(context, Action.SUBSCRIBE, clientHandle, topics));
    }
    catch (MqttSecurityException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to subscribe to" + topic + " the client with the handle " + clientHandle, e);
    }
    catch (MqttException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to subscribe to" + topic + " the client with the handle " + clientHandle, e);
    }
  }

  /**
   * Publish the message the user has specified
   */
  private void publish()
  {
    String topic = ((EditText) connectionDetails.findViewById(R.id.lastWillTopic))
        .getText().toString();

    ((EditText) connectionDetails.findViewById(R.id.lastWillTopic)).getText().clear();

    String message = ((EditText) connectionDetails.findViewById(R.id.lastWill)).getText()
        .toString();

    ((EditText) connectionDetails.findViewById(R.id.lastWill)).getText().clear();

    RadioGroup radio = (RadioGroup) connectionDetails.findViewById(R.id.qosRadio);
    int checked = radio.getCheckedRadioButtonId();
    int qos = ActivityConstants.defaultQos;

    switch (checked) {
      case R.id.qos0 :
        qos = 0;
        break;
      case R.id.qos1 :
        qos = 1;
        break;
      case R.id.qos2 :
        qos = 2;
        break;
    }

    boolean retained = ((CheckBox) connectionDetails.findViewById(R.id.retained))
        .isChecked();

    String[] args = new String[2];
    args[0] = message;
    args[1] = topic;

    try {
      Connections.getInstance().getConnection(clientHandle).getClient()
          .publish(topic, message.getBytes(), qos, retained, null, new ActionListener(context, Action.PUBLISH, clientHandle, args));
    }
    catch (MqttSecurityException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to publish a messged from the client with the handle " + clientHandle, e);
    }
    catch (MqttException e) {
      Log.e(this.getClass().getCanonicalName(), "Failed to publish a messged from the client with the handle " + clientHandle, e);
    }

  }

  /**
   * Create a new client and connect
   */
  private void createAndConnect()
  {
    Intent createConnection;

    //start a new activity to gather information for a new connection
    createConnection = new Intent();
    createConnection.setClassName(
        clientConnections.getApplicationContext(),
        "com.ibm.msg.android.NewConnection");

    clientConnections.startActivityForResult(createConnection,
        ActivityConstants.connect);
  }

  /**
   * Enables logging in the Paho MQTT client
   */
  private void enablePahoLogging() {

    try {
      InputStream logPropStream = context.getResources().openRawResource(R.raw.jsr47android);
      LogManager.getLogManager().readConfiguration(logPropStream);
      logging = true;
      clientConnections.invalidateOptionsMenu();
    }
    catch (IOException e) {
      Log.e("MqttClientAndroidService",
          "Error reading logging parameters", e);
    }

  }

  /**
   * Disables logging in the Paho MQTT client
   */
  private void disablePahoLogging() {
    LogManager.getLogManager().reset();
    logging = false;
  }

  /**
   * Writes the log to disk in the directory returned by 
   * {@link System#getProperty(String)} with the parameter
   * "java.io.tmpdir"
   * 
   */
  private void writeLog() {

    Map<String, Connection> connections = Connections.getInstance().getConnections();
    for (Map.Entry<String, Connection> entry : connections.entrySet())
    {
      entry.getValue().getClient().getDebug().dumpBaseDebug();
    }

    clientConnections.invalidateOptionsMenu();
  }

}
