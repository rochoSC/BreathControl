/*
* Author:       Roger Fernando Solis Castilla
* Date:         06/16/2015
* Description:  Activity that connects the bioharness and do the first measure
* */

package psi.tamu.controlyourbreath;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import zephyr.android.BioHarnessBT.*;


public class BHConnectedEventListener extends ConnectListenerImpl{

    private Handler hdlrOldHandler; //To keep the oldest one
    private Handler hdlrNewHandler; //The one that will be used

    //Identifier of the packet
    final int GP_MSG_ID = 0x20;

    //Identifier of the measure
    private final int RESPIRATION_RATE = 0x101;

    //Creating the different Objects for different types of Packets
    private GeneralPacketInfo GPInfo = new GeneralPacketInfo();
    private PacketTypeRequest RqPacketType = new PacketTypeRequest();

    /*
     * Constructor
     */
    public BHConnectedEventListener(Handler handler, Handler _NewHandler) {
        super(handler, null);
        hdlrOldHandler= handler;
        hdlrNewHandler = _NewHandler;
    }

    /*
    * Main function of the even listener
    * */
    public void Connected(ConnectedEvent<BTClient> eventArgs) {
        Log.i("CnctdEvntLstnr","Connected to BH: " + eventArgs.getSource().getDevice().getName());

		//Use this object to enable or disable the different Packet types
        RqPacketType.GP_ENABLE = true;
        RqPacketType.BREATHING_ENABLE = true;
        RqPacketType.LOGGING_ENABLE = true;


        //Creates a new ZephyrProtocol object and passes it the BTComms object
        ZephyrProtocol zpProtocol = new ZephyrProtocol(eventArgs.getSource().getComms(), RqPacketType);

        //Event listener for packet reception
        zpProtocol.addZephyrPacketEventListener(new ZephyrPacketListener() {
            public void ReceivedPacket(ZephyrPacketEvent eventArgs) {
                ZephyrPacketArgs msg = eventArgs.getPacket();
                int MsgID = msg.getMsgID();
                byte [] DataArray = msg.getBytes();

                //Only the general case
                if (MsgID == GP_MSG_ID) {
                    //Displaying only the Respiration Rate
                    double RespRate = GPInfo.GetRespirationRate(DataArray);

                    Message text1 = hdlrNewHandler.obtainMessage(RESPIRATION_RATE);
                    Bundle b1 = new Bundle();
                    b1.putString("RespirationRate", String.valueOf(RespRate));
                    text1.setData(b1);
                    hdlrNewHandler.sendMessage(text1);
                }
            }
        });
    }
}
