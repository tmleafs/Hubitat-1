/**
 *  ****************  Event Watchdog Driver  ****************
 *
 *  Design Usage:
 *  This driver opens a webSocket to capture Event info.
 *
 *  Copyright 2019-2020 Bryan Turcotte (@bptworld)
 *  
 *  This App is free.  If you like and use this app, please be sure to mention it on the Hubitat forums!  Thanks.
 *
 *  Remember...I am not a programmer, everything I do takes a lot of time and research (then MORE research)!
 *  Donations are never necessary but always appreciated.  Donations to support development efforts are accepted via: 
 *
 *  Paypal at: https://paypal.me/bptworld
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  If modifying this project, please keep the above header intact and add your comments/credits below - Thank you! -  @BPTWorld
 *
 *  App and Driver updates can be found at https://github.com/bptworld/Hubitat
 *
 * ------------------------------------------------------------------------------------------------------------------------------
 *
 *  Special thanks to @dan.t and his sample code for making the websocket connection.
 *
 *  Changes:
 *
 *  1.0.8 - 08/06/20 - Still chasing the null
 *  1.0.7 - 08/06/20 - Added more logging
 *  1.0.6 - 08/06/20 - More changes
 *  1.0.5 - 08/05/20 - Lots of changes
 *  1.0.4 - 07/20/20 - Adjustments
 *  1.0.3 - 07/09/20 - Error trapping
 *  1.0.2 - 06/29/20 - Code improvements
 *  1.0.1 - 06/29/20 - Learned some new things
 *  1.0.0 - 06/28/20 - Initial release
 *
 */

import groovy.json.*
    
metadata {
	definition (name: "Event Watchdog Driver", namespace: "BPTWorld", author: "Bryan Turcotte", importUrl: "") {
   		capability "Actuator"
        capability "Initialize"
        
        command "close"
        command "clearData"
        command "keywordInfo"
        command "appStatus"
       
        attribute "status", "string"
        attribute "bpt-lastEventMessage", "string"       
        attribute "bpt-eventData", "string"        
        attribute "numOfCharacters", "number"
        attribute "keywordInfo", "string"
        attribute "appStatus", "string"
    }
    preferences() {    	
        section(){
            input name: "about", type: "paragraph", element: "paragraph", title: "<b>Log Watchdog Driver</b>", description: "ONLY click 'Clear Data' to clear the event data."
            input("disableConnection", "bool", title: "Disable Connection", required: true, defaultValue: false)
            input("fontSize", "text", title: "Font Size", required: true, defaultValue: "15")
			input("hourType", "bool", title: "Time Selection (Off for 24h, On for 12h)", required: false, defaultValue: false)
            input("logEnable", "bool", title: "Enable logging", required: true, defaultValue: false)
            input("traceEnable", "bool", title: "Enable Trace", required: true, defaultValue: false)
        }
    }
}

def installed(){
    log.info "Event Watchdog Driver has been Installed"
    clearData()
    initialize()
}

def updated() {
    log.info "Event Watchdog Driver has been Updated"
    initialize()
}

def initialize() {
    log.info "In initialize"
    if(disableConnection) {
        log.info "Event Watchdog Driver - webSocket Connection is Disabled by the Device"
    } else {
        log.info "Event Watchdog Driver - Connecting webSocket - (${state.version})"
        interfaces.webSocket.connect("ws://localhost:8080/eventsocket")
    }
}

void uninstalled() {
	interfaces.webSocket.close()
}

def close() {
    interfaces.webSocket.close()
    log.warn "Event Watchdog Driver - Closing webSocket"
}

def webSocketStatus(String socketStatus) {
    state.version = "1.0.8"
    if(logEnabled) log.debug "In webSocketStatus - socketStatus: ${socketStatus}"
	if(socketStatus.startsWith("status: open")) {
		log.warn "Event Watchdog Driver - Connected - (${state.version})"
        sendEvent(name: "status", value: "connected", displayed: true)
        pauseExecution(500)
        state.delay = null
        return
	} else if(socketStatus.startsWith("status: closing")) {
		log.warn "Event Watchdog Driver - Closing connection - (${state.version})"
        sendEvent(name: "status", value: "disconnected")
        return
	} else if(socketStatus.startsWith("failure:")) {
		log.warn "Event Watchdog Driver - Connection has failed with error [${socketStatus}]. - (${state.version})"
        sendEvent(name: "status", value: "disconnected", displayed: true)
        autoReconnectWebSocket()
	} else {
        log.warn "WebSocket error, reconnecting."
        autoReconnectWebSocket()
	}
}

def autoReconnectWebSocket() {
    state.delay = (state.delay ?: 0) + 30    
    if(state.delay > 600) state.delay = 600

    log.warn "Event Watchdog Driver - Connection lost, will try to reconnect in ${state.delay} seconds - (${state.version})"
    runIn(state.delay, initialize)
}

def keywordInfo(keys) {
    if(traceEnable) log.trace "In keywordInfo"
    
    if(keys) {
        def (keySet,keySetType,keyword1,sKeyword1,sKeyword2,sKeyword3,sKeyword4,nKeyword1,nKeyword2) = keys.split(";")
    
        state.keyValue = "${keySetType};${keyword1};${sKeyword1};${sKeyword2};${sKeyword3};${sKeyword4};${nKeyword1};${nKeyword2}"
        if(traceEnable) log.trace "In keywordInfo (${state.version}) - keyValue: ${state.keyValue}"

        state.keySetType = keySetType.toLowerCase()
        state.keyword = keyword1.toLowerCase()
        if(skeyword1) state.skeyword1 = skeyword1.toLowerCase()
        if(skeyword2) state.skeyword2 = skeyword2.toLowerCase()
        if(skeyword3) state.skeyword3 = skeyword3.toLowerCase()
        if(skeyword4) state.skeyword4 = skeyword4.toLowerCase()
        if(nkeyword1) state.nkeyword1 = nkeyword1.toLowerCase()
        if(nkeyword2) state.nkeyword2 = nkeyword2.toLowerCase()

        if(state.sKeyword1 == "-") state.sKeyword1 = null
        if(state.sKeyword2 == "-") state.sKeyword2 = null
        if(state.sKeyword3 == "-") state.sKeyword3 = null
        if(state.sKeyword4 == "-") state.sKeyword4 = null
        if(state.nKeyword1 == "-") state.nKeyword1 = null
        if(state.nKeyword2 == "-") state.nKeyword2 = null
    }
}

def parse(String description) {
    def aStatus = device.currentValue('appStatus')
    if(aStatus == "active") {
        theData = "${description}"
        
        // This is what the incoming data looks like
        //{"source":"DEVICE","name":"contact","displayName":"Kitchen Freezer Door Sensor","value":"open","unit":null,"deviceId":157,"hubId":null,"locationId":null,"installedAppId":null,"descriptionText":"Kitchen Freezer Door Sensor was opened"}

        def message =  new JsonSlurper().parseText(theData)
        
        // source, name, displayName,value, unit, deviceID, HubID, locationId, installedAppId, descriptionText
        
        if(state.keySetType) {          
            if(message.source) {
                displayName = message.displayName
                nameV = message.name.toLowerCase()
                sourceV = message.source.toLowerCase()
                //log.info "sourceV: ${sourceV}"

                if(sourceV == "device") {
                    displayName = message.displayName
                    nameV = message.name.toLowerCase()
                    //log.info "nameV: ${nameV} - keySetType: ${keySetType}"
                    if(nameV == state.keyword) log.warn "YES"
                } else {
                    nameV = "-"
                }
            }

            if(state.keySetType == "d" || state.keySetType == "k") {
                if(message.descriptionText) {
                    msgCheck = message.descriptionText.toLowerCase()
                }
            } else if(state.keySetType == "e") {
                msgCheck = nameV.toLowerCase()
            }
            if(msgCheck == null) msgCheck = "-----"
        }    
        
        if(msgCheck != null) {            
            //log.debug "msgCheck: ${msgCheck} - keyword: ${state.keyword}"

            state.kCheck1 = false
            state.kCheck2 = true
            state.match = false

            try {
                readyToGo = false
                if(msgCheck.contains("${state.keyword}")) {
                    if(traceEnable) {
                        keyword1a = state.keyword.replace("a","@").replace("e","3").replace("i","1").replace("o","0",).replace("u","^")
                        log.trace "In keyword (${state.version}) - Found msgCheck: ${keyword1a}"
                    }
                    readyToGo = true
                }

                if(readyToGo) {
                    if(state.sKeyword1 || state.sKeyword2 || state.sKeyword3 || state.sKeyword4) {
                        if(msgCheck.contains("${state.sKeyword1}")) {
                            if(traceEnable) log.trace "In Secondary Keyword1 - ${state.sKeyword1} Found! That's GOOD!"
                            state.kCheck1 = true
                        } else if(msgCheck.contains("${state.sKeyword2}")) {
                            if(traceEnable) log.trace "In Secondary Keyword2 - ${state.sKeyword2} Found! That's GOOD!"
                            state.kCheck1 = true
                        } else if(msgCheck.contains("${state.sKeyword3}")) {
                            if(traceEnable) log.trace "In Secondary Keyword1 - ${state.sKeyword3} Found! That's GOOD!"
                            state.kCheck1 = true
                        } else if(msgCheck.contains("${state.sKeyword4}")) {
                            if(traceEnable) log.trace "In Secondary Keyword4 - ${state.sKeyword4} Found! That's GOOD!"
                            state.kCheck1 = true
                        }       
                    } else {
                        state.kCheck1 = true
                    }

                    if(state.nKeyword1 || state.nKeyword2) {  
                        if(msgCheck.contains("${state.nKeyword1}")) {
                            if(traceEnable) log.trace "In Not Keyword1 - ${state.nKeyword1} found! That's BAD!"
                            state.kCheck2 = false
                        } else if(msgCheck.contains("${state.nKeyword2}")) {
                            if(traceEnable) log.trace "In Not Keyword2 - ${state.nKeyword2} found! That's BAD!"
                            state.kCheck2 = false
                        }
                    }

                    if(traceEnable) log.trace "In keyword - ${keyword1a} - kCheck1: ${state.kCheck1} - kCheck2: ${state.kCheck2}"
                    if(state.kCheck1 && state.kCheck2) {
                        state.match = true
                    } else {
                        state.match = false
                    }
                }

                if(state.match) {
                    if(traceEnable) log.trace "In keyword (${state.version}) - ${keyword1a} - Everything Passed!"

                    if(msgCheck == null || msgCheck == "null") {
                        if(traceEnable) log.warn "In keyword - Can't send msgV, description is null"
                    } else {
                        if(traceEnable) {
                            log.warn "In keyword - displayName: ${message.displayName} - descriptionText: ${message.descriptionText}"
                            log.warn "In keyword - Sending: ${message.descriptionText}"
                        }
                        if(message.descriptionText.contains("${displayName}")) {
                            newText = message.descriptionText
                        } else {
                            newText = "${displayName} - ${message.descriptionText}"
                        }
                        if(message.descriptionText != "null" && message.descriptionText != null) makeList(newText)
                    }
                }
            } catch (e) {
                if(traceEnable) log.trace "In parse - Error to follow!"
                log.error e
            }
        }
    }
}
 
def makeList(data) {
    def msgValue = data
    if(traceEnable) log.trace "In makeList (${state.version}) - working on - ${msgValue}"

    try {
        if(state.list == null) state.list = []

        getDateTime()
        last = "${newDate}::${msgValue}"
        state.list.add(0,last)  

        if(state.list) {
            listSize1 = state.list.size()
        } else {
            listSize1 = 0
        }

        int intNumOfLines = 10
        if (listSize1 > intNumOfLines) state.list.removeAt(intNumOfLines)
        String result1 = state.list.join(",")
        def lines = result1.split(",")

        theData = "<div style='overflow:auto;height:90%'><table style='text-align:left;font-size:${fontSize}px'><tr><td width=10%><td width=1%><td width=89%>"

        for (i=0;i<intNumOfLines && i<listSize1;i++) {
            combined = theData.length() + lines[i].length() + 16
            if(combined < 1000) {
                def (theTime, theMsg) = lines[i].split("::") 
                theData += "<tr><td>${theTime}<td> - <td>${theMsg}"
            }
        }

        theData += "</table></div>"
        if(logEnable) log.debug "theData - ${theData.replace("<","!")}"       

        dataCharCount1 = theData.length()
        if(dataCharCount1 <= 1024) {
            if(logEnable) log.debug "Event Watchdog Attribute - theData - ${dataCharCount1} Characters"
        } else {
            theData = "Event Watchdog - Too many characters to display on Dashboard (${dataCharCount1})"
        }

        sendEvent(name: "bpt-eventData", value: theData, displayed: true)
        sendEvent(name: "numOfCharacters", value: dataCharCount1, displayed: true)
        sendEvent(name: "bpt-lastEventMessage", value: msgValue, isStateChange: true)
    }
    catch(e1) {
        log.error "Event Watchdog Driver - In makeList - Error to follow!"
        log.error e1  
    }
}

def appStatus(data){
	if(logEnable) log.debug "Event Watchdog Driver - In appStatus"
    sendEvent(name: "appStatus", value: data, displayed: true)
}

def clearData(){
	if(logEnable) log.debug "Log Watchdog Driver - Clearing the data"
    msgValue = "-"
    logCharCount = "0"
    
    state.list = []
    sendEvent(name: "bpt-eventData", value: state.list, displayed: true)
	
    sendEvent(name: "bpt-lastEventMessage", value: msgValue, isStateChange: true)
    sendEvent(name: "numOfCharacters", value: logCharCount, displayed: true)
}

def getDateTime() {
	def date = new Date()
	if(hourType == false) newDate=date.format("MM-d HH:mm")
	if(hourType == true) newDate=date.format("MM-d hh:mm")
    return newDate
}
