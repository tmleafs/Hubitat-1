/**
 *  ****************  Abacus Time Traveler Child ****************
 *
 *  Design Usage:
 *  Track how long a Device has been active. Displays Daily, Weekly, Monthly and Yearly Timers!
 *
 *  Copyright 2020 Bryan Turcotte (@bptworld)
 *
 *  This App is free.  If you like and use this app, please be sure to mention it on the Hubitat forums!  Thanks.
 *
 *  Remember...I am not a programmer, everything I do takes a lot of time and research!
 *  Donations are never necessary but always appreciated.  Donations to support development efforts are accepted via: 
 *
 *  Paypal at: https://paypal.me/bptworld
 * 
 *  Unless noted in the code, ALL code contained within this app is mine. You are free to change, ripout, copy, modify or
 *  otherwise use the code in anyway you want. This is a hobby, I'm more than happy to share what I have learned and help
 *  the community grow. Have FUN with it!
 * 
 *-------------------------------------------------------------------------------------------------------------------
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
 *  Changes:
 *
 *  1.0.2 - 08/02/20 - Cosmetic Changes
 *  1.0.1 - 07/09/20 - Fixed Disable switch
 *  1.0.0 - 06/26/20 - Initial release.
 *
 */

import groovy.time.TimeCategory
import java.text.SimpleDateFormat
    
def setVersion(){
    state.name = "Abacus Time Traveler"
	state.version = "1.0.2"
}

definition(
    name: "Abacus Time Traveler Child",
    namespace: "BPTWorld",
    author: "Bryan Turcotte",
    description: "Track how long a Device has been active. Displays Daily, Weekly, Monthly and Yearly Timers!",
    category: "Convenience",
	parent: "BPTWorld:Abacus Time Traveler",
    iconUrl: "",
    iconX2Url: "",
    iconX3Url: "",
	importUrl: "",
)

preferences {
    page(name: "pageConfig")
    page name: "pageCounts", title: "", install: false, uninstall: false, nextPage: "pageConfig"
    page name: "resetCounts", title: "", install: false, uninstall: false, nextPage: "pageConfig"
}

def pageConfig() {
    dynamicPage(name: "", title: "", install: true, uninstall: true) {
	display()
		section("Instructions:", hideable: true, hidden: true) {
			paragraph "<b>Information</b>"
			paragraph "Daily timers are reset each night.<br>Weekly timers are reset each Saturday night.<br>Monthly timers are reset on the last day of each month.<br>Yearly timers reset on Dec 31.<br>* All timer resets happen around Midnight."
		}
        
        section(getFormat("header-green", "${getImage("Blank")}"+" Data Device")) {
            paragraph "Each child app needs a virtual device to store the data."
            input "useExistingDevice", "bool", title: "Use existing device (off) or have Abacus create a new one for you (on)", defaultValue:false, submitOnChange:true
            if(useExistingDevice) {
			    input "dataName", "text", title: "Enter a name for this Data Device (ie. 'ATT - Timers')", required:true, submitOnChange:true
                paragraph "<b>A device will automaticaly be created for you as soon as you click outside of this field.</b>"
                if(dataName) createDataChildDevice()
                if(statusMessageD == null) statusMessageD = "Waiting on status message..."
                paragraph "${statusMessageD}"
            }
            input "dataDevice", "capability.actuator", title: "Virtual Device to send the data to", required:true, multiple:false
            if(!useExistingDevice) {
                app.removeSetting("dataName")
                paragraph "<small>* Device must use the 'Abacus Counting Machine Driver'.</small>"
            }
        }
        
		section(getFormat("header-green", "${getImage("Blank")}"+" Most Common Devices")) {
            input "contactEvent", "capability.contactSensor", title: "Contact Sensor(s) to track time", submitOnChange: true, required: false, multiple: true			
			input "motionEvent", "capability.motionSensor", title: "Motion sensor(s) to track time", submitOnChange: true, required: false, multiple: true
			input "switchEvent", "capability.switch", title: "Switch Device(s) to track time", submitOnChange: true, required: false, multiple: true
			input "thermostatEvent", "capability.thermostat", title: "Thermostat(s) to track time", submitOnChange: true, required: false, multiple: true
		}
        
        section(getFormat("header-green", "${getImage("Blank")}"+" Reset timers")) {
            input "removeOrphanedDevices", "bool", title: "Remove Orphaned Devices from Maps", defaultValue:false, submitOnChange:true
            if(removeOrphanedDevices) { 
                removeOrphanedDevices()
                app?.updateSetting("removeOrphanedDevices",[value:"false",type:"bool"])
            }
			href "resetCounts", title: "Reset Timers", description: "Click here reset options."
    	}
        
        section(getFormat("header-green", "${getImage("Blank")}"+" Reports")) {
			href "pageCounts", title: "Abacus Time Traveler Report", description: "Click here to view the report."
		}
        
        section(getFormat("header-green", "${getImage("Blank")}"+" App Control")) {
            input "pauseApp", "bool", title: "Pause App", defaultValue:false, submitOnChange:true            
            if(pauseApp) {
                if(app.label) {
                    if(!app.label.contains(" (Paused)")) {
                        app.updateLabel(app.label + " (Paused)")
                    }
                }
            } else {
                if(app.label) {
                    app.updateLabel(app.label - " (Paused)")
                }
            }
            paragraph "This app can be enabled/disabled by using a switch. The switch can also be used to enable/disable several apps at the same time."
            input "disableSwitch", "capability.switch", title: "Switch Device(s) to Enable / Disable this app", submitOnChange:true, required:false, multiple:true
        }
        
		section(getFormat("header-green", "${getImage("Blank")}"+" Maintenance")) {
            label title: "Enter a name for this child app", required:false, submitOnChange:true
            input "fontSize", "number", title: "Dashboard Tile Font Size", defaultValue:15, submitOnChange:true
			input "logEnable", "bool", title: "Enable Debug Logging", description: "debug", defaultValue:false, submitOnChange:true
    	}
		display2()
	}
}

def pageCounts() {
	dynamicPage(name: "pageCounts", title: "", install:false, uninstall:false) {
        display()
        section(getFormat("header-green", "${getImage("Blank")}"+" Contact Sensor Timers")) {
            if(contactEvent) {
                contactMap1 = dataDevice.currentValue("bpt-abacusContact1")
                contactMap2 = dataDevice.currentValue("bpt-abacusContact2")
                contactMap3 = dataDevice.currentValue("bpt-abacusContact3")

                contactCount1 = dataDevice.currentValue("abacusContactCount1")
                contactCount2 = dataDevice.currentValue("abacusContactCount2")
                contactCount3 = dataDevice.currentValue("abacusContactCount3")

                if(contactCount1) a1 = contactCount1.toInteger()
                if(contactCount2) a2 = contactCount2.toInteger()
                if(contactCount3) a3 = contactCount3.toInteger()

                if(logEnable) log.debug "In reportHandler - contact - a1: ${a1} - a2: ${a2} - a3: ${a3}"

                if(a1 >= 42) {
                    paragraph "${contactMap1}"
                    if(a1 <= 1024) paragraph "Tile Count: <span style='color:green'>${a1}</span>"
                    if(a1 > 1024) paragraph "<span style='color:red'>Tile Count: ${a1}</span>"
                    paragraph "<hr>"
                }
                if(a2 >= 42) {
                    paragraph "${contactMap2}"
                    if(a2 <= 1024) paragraph "Tile Count: <span style='color:green'>${a2}</span>"
                    if(a2 > 1024) paragraph "<span style='color:red'>Tile Count: ${a2}</span>"
                    paragraph "<hr>"
                }
                if(a3 >= 42) {
                    paragraph "${contactMap3}"
                    if(a3 <= 1024) paragraph "Tile Count: <span style='color:green'>${a3}</span>"
                    if(a3 > 1024) paragraph "<span style='color:red'>Tile Count: ${a3}</span>"
                }

                if(a1 < 42 && a2 < 42 && a3 < 42) {
                    paragraph "<div style='font-size:${fontSize}px'>Contact Report<br>Nothing to report</div>"
                }
            } else {
                paragraph "No devices have been selected for this option."
            }
        }
        
        section(getFormat("header-green", "${getImage("Blank")}"+" Motion Sensor Timers")) {
            if(motionEvent) {
                motionMap1 = dataDevice.currentValue("bpt-abacusMotion1")
                motionMap2 = dataDevice.currentValue("bpt-abacusMotion2")
                motionMap3 = dataDevice.currentValue("bpt-abacusMotion3")

                motionCount1 = dataDevice.currentValue("abacusMotionCount1")
                motionCount2 = dataDevice.currentValue("abacusMotionCount2")
                motionCount3 = dataDevice.currentValue("abacusMotionCount3")

                if(motionCount1) a1 = motionCount1.toInteger()
                if(motionCount2) a2 = motionCount2.toInteger()
                if(motionCount3) a3 = motionCount3.toInteger()

                if(logEnable) log.debug "In reportHandler - motion - a1: ${a1} - a2: ${a2} - a3: ${a3}"

                if(a1 >= 42) {
                    paragraph "${motionMap1}"
                    if(a1 <= 1024) paragraph "Tile Count: <span style='color:green'>${a1}</span>"
                    if(a1 > 1024) paragraph "<span style='color:red'>Tile Count: ${a1}</span>"
                    paragraph "<hr>"
                }
                if(a2 >= 42) {
                    paragraph "${motionMap2}"
                    if(a2 <= 1024) paragraph "Tile Count: <span style='color:green'>${a2}</span>"
                    if(a2 > 1024) paragraph "<span style='color:red'>Tile Count: ${a2}</span>"
                    paragraph "<hr>"
                }
                if(a3 >= 42) {
                    paragraph "${motionMap3}"
                    if(a3 <= 1024) paragraph "Tile Count: <span style='color:green'>${a3}</span>"
                    if(a3 > 1024) paragraph "<span style='color:red'>Tile Count: ${a3}</span>"
                }

                if(a1 < 42 && a2 < 42 && a3 < 42) {
                    paragraph "<div style='font-size:${fontSize}px'>Motion Report<br>Nothing to report</div>"
                }
            } else {
                paragraph "No devices have been selected for this option."
            }
        }

        section(getFormat("header-green", "${getImage("Blank")}"+" Switch Timers")) {
            if(switchEvent) {
                switchMap1 = dataDevice.currentValue("bpt-abacusSwitch1")
                switchMap2 = dataDevice.currentValue("bpt-abacusSwitch2")
                switchMap3 = dataDevice.currentValue("bpt-abacusSwitch3")

                switchCount1 = dataDevice.currentValue("abacusSwitchCount1")
                switchCount2 = dataDevice.currentValue("abacusSwitchCount2")
                switchCount3 = dataDevice.currentValue("abacusSwitchCount3")

                if(switchCount1) a1 = switchCount1.toInteger()
                if(switchCount2) a2 = switchCount2.toInteger()
                if(switchCount3) a3 = switchCount3.toInteger()

                if(logEnable) log.debug "In reportHandler - Switch - a1: ${a1} - a2: ${a2} - a3: ${a3}"

                if(a1 >= 42) {
                    paragraph "${switchMap1}"
                    if(a1 <= 1024) paragraph "Tile Count: <span style='color:green'>${a1}</span>"
                    if(a1 > 1024) paragraph "<span style='color:red'>Tile Count: ${a1}</span>"
                    paragraph "<hr>"
                }
                if(a2 >= 42) {
                    paragraph "${switchMap2}"
                    if(a2 <= 1024) paragraph "Tile Count: <span style='color:green'>${a2}</span>"
                    if(a2 > 1024) paragraph "<span style='color:red'>Tile Count: ${a2}</span>"
                    paragraph "<hr>"
                }
                if(a3 >= 42) {
                    paragraph "${switchMap3}"
                    if(a3 <= 1024) paragraph "Tile Count: <span style='color:green'>${a3}</span>"
                    if(a3 > 1024) paragraph "<span style='color:red'>Tile Count: ${a3}</span>"
                }

                if(a1 < 42 && a2 < 42 && a3 < 42) {
                    paragraph "<div style='font-size:${fontSize}px'>Switch Report<br>Nothing to report</div>"
                }
            } else {
                paragraph "No devices have been selected for this option."
            }
        }
         
        section(getFormat("header-green", "${getImage("Blank")}"+" Thermostat Timers")) {
            if(thermostatEvent) {
                thermostatMap1 = dataDevice.currentValue("bpt-abacusThermostat1")
                thermostatMap2 = dataDevice.currentValue("bpt-abacusThermostat2")
                thermostatMap3 = dataDevice.currentValue("bpt-abacusThermostat3")

                thermostatCount1 = dataDevice.currentValue("abacusThermostatCount1")
                thermostatCount2 = dataDevice.currentValue("abacusThermostatCount2")
                thermostatCount3 = dataDevice.currentValue("abacusThermostatCount3")

                if(thermostatCount1) a1 = thermostatCount1.toInteger()
                if(thermostatCount2) a2 = thermostatCount2.toInteger()
                if(thermostatCount3) a3 = thermostatCount3.toInteger()

                if(logEnable) log.debug "In reportHandler - Thermostat - a1: ${a1} - a2: ${a2} - a3: ${a3}"

                if(a1 >= 42) {
                    paragraph "${thermostatMap1}"
                    if(a1 <= 1024) paragraph "Tile Count: <span style='color:green'>${a1}</span>"
                    if(a1 > 1024) paragraph "<span style='color:red'>Tile Count: ${a1}</span>"
                    paragraph "<hr>"
                }
                if(a2 >= 42) {
                    paragraph "${thermostatMap2}"
                    if(a2 <= 1024) paragraph "Tile Count: <span style='color:green'>${a2}</span>"
                    if(a2 > 1024) paragraph "<span style='color:red'>Tile Count: ${a2}</span>"
                    paragraph "<hr>"
                }
                if(a3 >= 42) {
                    paragraph "${thermostatMap3}"
                    if(a3 <= 1024) paragraph "Tile Count: <span style='color:green'>${a3}</span>"
                    if(a3 > 1024) paragraph "<span style='color:red'>Tile Count: ${a3}</span>"
                }

                if(a1 < 42 && a2 < 42 && a3 < 42) {
                    paragraph "<div style='font-size:${fontSize}px'>Thermostat Report<br>Nothing to report</div>"
                }
            } else {
                paragraph "No devices have been selected for this option."
            }
        }   
    }
}

def resetCounts() {
	dynamicPage(name: "resetCounts", title: "", install:false, uninstall:false) {
        display()
        section(getFormat("header-green", "${getImage("Blank")}"+" Reset Timers")) {
            paragraph "<b>Use with caution - This can NOT be undone!</b>"
            input "resetAllCounts", "bool", title: "Reset ALL Timers", description: "Reset All", defaultValue:false, submitOnChange:true
            if(resetAllCounts) {
	            state.clear()
                state.contactMap = [:]
                state.motionMap = [:]
                state.switchMap = [:]
                state.thermostatMap = [:]
                app?.updateSetting("resetAllCounts",[value:"false",type:"bool"])
            }
        }      
    }
}

def uninstalled() {
	removeChildDevices(getChildDevices())
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}

def installed() {
	log.info "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	if(logEnable) log.debug "Updated with settings: ${settings}"
	unsubscribe()
    unschedule()
    if(logEnable) runIn(3600, logsOff)
	initialize()
}

def initialize() {
	if(logEnable) log.debug "In initialize (${state.version})"
    checkEnableHandler()
    if(pauseApp || state.eSwitch) {
        log.info "${app.label} is Paused or Disabled"
    } else {
        setDefaults()
        subscribe(contactEvent, "contact", contactHandler)
        subscribe(motionEvent, "motion", motionHandler)
        subscribe(switchEvent, "switch", switchHandler)
        subscribe(thermostatEvent, "thermostatOperatingState", thermostatHandler)

        schedule("0 57 23 1/1 * ? *", resetDayHandler, [data: "D"])
        schedule("0 58 23 ? * SAT *", resetWeekHandler, [data: "W"])
        schedule("0 59 23 L * ? *", resetMonthHandler, [data: "M"])
        schedule("0 0 0 1 1 ? *", resetYearHandler, [data: "Y"])
    }
}

def resetDayHandler(data) {
    resetCountsHandler(data)
}

def resetWeekHandler(data) {
    resetCountsHandler(data)
}

def resetMonthHandler(data) {
    resetCountsHandler(data)
}

def resetYearHandler(data) {
    resetCountsHandler(data)
}

def contactHandler(evt) {
    checkEnableHandler()
    if(pauseApp || state.eSwitch) {
        log.info "${app.label} is Paused or Disabled"
    } else {
        if(logEnable) log.debug "In contactHandler (${state.version})"
        state.contactStatus = evt.value
        if(state.contactPrevMap == null) state.contactPrevMap = [:]
        if(state.contactMap == null) state.contactMap = [:]

        if(state.contactStatus == "open") {
            def now1 = new Date()
            prev = now1.getTime()
            state.contactPrevMap.put(evt.displayName, prev)
            if(logEnable) log.debug "In contactHandler - ${evt.displayName} became ${state.contactStatus} at ${now1}"
        }

        if(state.contactStatus == "closed") {
            prev = state.contactPrevMap.get(evt.displayName, prev)
            if(prev) {
                def now2 = new Date()
                if(logEnable) log.debug "In contactHandler - ${evt.displayName} became ${state.contactStatus} at ${now2}"
                long unxNow = now2.getTime()
                long unxPrev = prev
                unxNow = unxNow/1000 
                unxPrev = unxPrev/1000
                timeDiff = (unxNow-unxPrev)
                if(logEnable) log.debug "In contactHandler - ${evt.displayName}: timeDiff in Seconds: ${timeDiff}"

                contactEvent.each { it ->
                    String theEvent = evt.displayName
                    String theName = it

                    if(logEnable) log.debug "In contactHandler - Comparing theEvent: ${theEvent} vs ${theName}"
                    if(theEvent == theName) { 
                        if(logEnable) log.debug "In contactHandler - MATCH!"
                        theCounts = state.contactMap.get(it.displayName)

                        if(theCounts) {     
                            def (theDay,theWeek,theMonth,theYear) = theCounts.split(",")               
                            if(logEnable) log.debug "In contactHandler - BEFORE - ${theName} - day: ${theDay} - week: ${theWeek} - month: ${theMonth} - year: ${theYear}"

                            newDay = theDay.toInteger() + timeDiff
                            newWeek = theWeek.toInteger() + timeDiff
                            newMonth = theMonth.toInteger() + timeDiff
                            newYear = theYear.toInteger() + timeDiff

                            if(logEnable) log.debug "In contactHandler - AFTER - ${theName} - day: ${newDay} - week: ${newWeek} - month: ${newMonth} - year: ${newYear}"
                            newValues = "${newDay},${newWeek},${newMonth},${newYear}"
                            state.contactMap.put(theName, newValues)

                            if(logEnable) log.debug "In contactHandler - UPDATED - ${theName} - day: ${newDay} - week: ${newWeek} - month: ${newMonth} - year: ${newYear}"
                        } else {
                            newDay = timeDiff;newWeek = timeDiff;newMonth = timeDiff;newYear = timeDiff               
                            newValues = "${newDay},${newWeek},${newMonth},${newYear}"
                            state.contactMap.put(theName, newValues)                
                            if(logEnable) log.debug "In contactHandler - ADDED - ${theName} - day: ${newDay} - week: ${newWeek} - month: ${newMonth} - year: ${newYear}"
                        }
                    }
                }
                comingFrom = "Contact Sensor"
                buildMaps(comingFrom)
            }
        }
    }
}

def motionHandler(evt) {
    checkEnableHandler()
    if(pauseApp || state.eSwitch) {
        log.info "${app.label} is Paused or Disabled"
    } else {
        if(logEnable) log.debug "In motionHandler (${state.version})"
        state.motionStatus = evt.value
        if(state.motionPrevMap == null) state.motionPrevMap = [:]
        if(state.motionMap == null) state.motionMap = [:]

        if(state.motionStatus == "active") {
            def now1 = new Date()
            prev = now1.getTime()
            state.motionPrevMap.put(evt.displayName, prev)
            if(logEnable) log.debug "In motionHandler - ${evt.displayName} became ${state.motionStatus} at ${now1}"
        }

        if(state.motionStatus == "inactive") {
            prev = state.motionPrevMap.get(evt.displayName, prev)
            if(prev) {
                def now2 = new Date()
                if(logEnable) log.debug "In motionHandler - ${evt.displayName} became ${state.motionStatus} at ${now2}"
                long unxNow = now2.getTime()
                long unxPrev = prev
                unxNow = unxNow/1000 
                unxPrev = unxPrev/1000
                timeDiff = (unxNow-unxPrev)
                if(logEnable) log.debug "In motionHandler - ${evt.displayName}: timeDiff in Seconds: ${timeDiff}"

                motionEvent.each { it ->
                    String theEvent = evt.displayName
                    String theName = it

                    if(logEnable) log.debug "In motionHandler - Comparing theEvent: ${theEvent} vs ${theName}"
                    if(theEvent == theName) { 
                        if(logEnable) log.debug "In motionHandler - MATCH!"
                        theCounts = state.motionMap.get(it.displayName)

                        if(theCounts) {     
                            def (theDay,theWeek,theMonth,theYear) = theCounts.split(",")               
                            if(logEnable) log.debug "In motionHandler - BEFORE - ${theName} - day: ${theDay} - week: ${theWeek} - month: ${theMonth} - year: ${theYear}"

                            newDay = theDay.toInteger() + timeDiff
                            newWeek = theWeek.toInteger() + timeDiff
                            newMonth = theMonth.toInteger() + timeDiff
                            newYear = theYear.toInteger() + timeDiff

                            if(logEnable) log.debug "In motionHandler - AFTER - ${theName} - day: ${newDay} - week: ${newWeek} - month: ${newMonth} - year: ${newYear}"
                            newValues = "${newDay},${newWeek},${newMonth},${newYear}"
                            state.motionMap.put(theName, newValues)

                            if(logEnable) log.debug "In motionHandler - UPDATED - ${theName} - day: ${newDay} - week: ${newWeek} - month: ${newMonth} - year: ${newYear}"
                        } else {
                            newDay = timeDiff;newWeek = timeDiff;newMonth = timeDiff;newYear = timeDiff              
                            newValues = "${newDay},${newWeek},${newMonth},${newYear}"
                            state.motionMap.put(theName, newValues)                
                            if(logEnable) log.debug "In motionHandler - ADDED - ${theName} - day: ${newDay} - week: ${newWeek} - month: ${newMonth} - year: ${newYear}"
                        }
                    }     
                }
                comingFrom = "Motion Sensor"
                buildMaps(comingFrom)
            }
        }
    }
}

def switchHandler(evt) {
    checkEnableHandler()
    if(pauseApp || state.eSwitch) {
        log.info "${app.label} is Paused or Disabled"
    } else {
        if(logEnable) log.debug "In switchHandler (${state.version})"
        state.switchStatus = evt.value
        if(state.switchPrevMap == null) state.switchPrevMap = [:]
        if(state.switchMap == null) state.switchMap = [:]

        if(state.switchStatus == "on") {
            def now1 = new Date()
            prev = now1.getTime()
            state.switchPrevMap.put(evt.displayName, prev)
            if(logEnable) log.debug "In switchHandler - ${evt.displayName} became ${state.switchStatus} at ${now1}"
        }

        if(state.switchStatus == "off") {
            prev = state.switchPrevMap.get(evt.displayName, prev)
            if(prev) {
                def now2 = new Date()
                if(logEnable) log.debug "In switchHandler - ${evt.displayName} became ${state.switchStatus} at ${now2}"
                long unxNow = now2.getTime()
                long unxPrev = prev
                unxNow = unxNow/1000 
                unxPrev = unxPrev/1000
                timeDiff = (unxNow-unxPrev)
                if(logEnable) log.debug "In switchHandler - ${evt.displayName}: timeDiff in Seconds: ${timeDiff}"

                switchEvent.each { it ->
                    String theEvent = evt.displayName
                    String theName = it

                    if(logEnable) log.debug "In switchHandler - Comparing theEvent: ${theEvent} vs ${theName}"
                    if(theEvent == theName) { 
                        if(logEnable) log.debug "In switchHandler - MATCH!"
                        theCounts = state.switchMap.get(it.displayName)

                        if(theCounts) {     
                            def (theDay,theWeek,theMonth,theYear) = theCounts.split(",")               
                            if(logEnable) log.debug "In switchHandler - BEFORE - ${theName} - day: ${theDay} - week: ${theWeek} - month: ${theMonth} - year: ${theYear}"

                            newDay = theDay.toInteger() + timeDiff
                            newWeek = theWeek.toInteger() + timeDiff
                            newMonth = theMonth.toInteger() + timeDiff
                            newYear = theYear.toInteger() + timeDiff

                            if(logEnable) log.debug "In switchHandler - AFTER - ${theName} - day: ${newDay} - week: ${newWeek} - month: ${newMonth} - year: ${newYear}"
                            newValues = "${newDay},${newWeek},${newMonth},${newYear}"
                            state.switchMap.put(theName, newValues)

                            if(logEnable) log.debug "In switchHandler - UPDATED - ${theName} - day: ${newDay} - week: ${newWeek} - month: ${newMonth} - year: ${newYear}"
                        } else {
                            newDay = timeDiff;newWeek = timeDiff;newMonth = timeDiff;newYear = timeDiff               
                            newValues = "${newDay},${newWeek},${newMonth},${newYear}"
                            state.switchMap.put(theName, newValues)                
                            if(logEnable) log.debug "In switchHandler - ADDED - ${theName} - day: ${newDay} - week: ${newWeek} - month: ${newMonth} - year: ${newYear}"
                        }
                    }     
                }
                comingFrom = "Switch"
                buildMaps(comingFrom)
            }
        }
    }
}

def thermostatHandler(evt) {
    checkEnableHandler()
    if(pauseApp || state.eSwitch) {
        log.info "${app.label} is Paused or Disabled"
    } else {
        if(logEnable) log.debug "In thermostatHandler (${state.version})"
        state.thermostatStatus = evt.value
        if(state.thermostatPrevMap == null) state.thermostatPrevMap = [:]
        if(state.thermostatMap == null) state.thermostatMap = [:]

        if(state.thermostatStatus != "idle") {
            def now1 = new Date()
            prev = now1.getTime()
            state.thermostatPrevMap.put(evt.displayName, prev)
            if(logEnable) log.debug "In thermostatHandler - ${evt.displayName} became ${state.thermostatStatus} at ${now1}"
        }

        if(state.thermostatStatus == "idle") {
            prev = state.thermostatPrevMap.get(evt.displayName, prev)
            if(prev) {
                def now2 = new Date()
                if(logEnable) log.debug "In thermostatHandler - ${evt.displayName} became ${state.thermostatStatus} at ${now2}"
                long unxNow = now2.getTime()
                long unxPrev = prev
                unxNow = unxNow/1000 
                unxPrev = unxPrev/1000
                timeDiff = (unxNow-unxPrev)
                if(logEnable) log.debug "In thermostatHandler - ${evt.displayName}: timeDiff in Seconds: ${timeDiff}"

                thermostatEvent.each { it ->
                    String theEvent = evt.displayName
                    String theName = it

                    if(logEnable) log.debug "In thermostatHandler - Comparing theEvent: ${theEvent} vs ${theName}"
                    if(theEvent == theName) { 
                        if(logEnable) log.debug "In thermostatHandler - MATCH!"
                        theCounts = state.thermostatMap.get(it.displayName)

                        if(theCounts) {     
                            def (theDay,theWeek,theMonth,theYear) = theCounts.split(",")               
                            if(logEnable) log.debug "In thermostatHandler - BEFORE - ${theName} - day: ${theDay} - week: ${theWeek} - month: ${theMonth} - year: ${theYear}"

                            newDay = theDay.toInteger() + timeDiff
                            newWeek = theWeek.toInteger() + timeDiff
                            newMonth = theMonth.toInteger() + timeDiff
                            newYear = theYear.toInteger() + timeDiff

                            if(logEnable) log.debug "In thermostatHandler - AFTER - ${theName} - day: ${newDay} - week: ${newWeek} - month: ${newMonth} - year: ${newYear}"
                            newValues = "${newDay},${newWeek},${newMonth},${newYear}"
                            state.thermostatMap.put(theName, newValues)

                            if(logEnable) log.debug "In thermostatHandler - UPDATED - ${theName} - day: ${newDay} - week: ${newWeek} - month: ${newMonth} - year: ${newYear}"
                        } else {
                            newDay = timeDiff;newWeek = timeDiff;newMonth = timeDiff;newYear = timeDiff               
                            newValues = "${newDay},${newWeek},${newMonth},${newYear}"
                            state.thermostatMap.put(theName, newValues)                
                            if(logEnable) log.debug "In thermostatHandler - ADDED - ${theName} - day: ${newDay} - week: ${newWeek} - month: ${newMonth} - year: ${newYear}"
                        }
                    }     
                }
                comingFrom = "Thermostat"
                buildMaps(comingFrom)
            }
        }
    }
}

def resetCountsHandler(data) {
    if(logEnable) log.debug "In resetCountsHandler (${state.version})"
    dataValue = data
    
    if(logEnable) log.debug "In resetCountsHandler - Resetting Contact - dataValue: ${dataValue}"    
    contactEvent.each { it ->
        String theName = it       
        theCounts = state.contactMap.get(it.displayName)
        if(theCounts) {     
            def (theDay,theWeek,theMonth,theYear) = theCounts.split(",")
            theZero = 0
            if(dataValue == "D") newValues = "${theZero},${theWeek},${theMonth},${theYear}"
            if(dataValue == "W") newValues = "${theZero},${theZero},${theMonth},${theYear}"
            if(dataValue == "M") newValues = "${theZero},${theZero},${theZero},${theYear}"
            if(dataValue == "Y") newValues = "${theZero},${theZero},${theZero},${theZero}"
            if(logEnable) log.debug "In resetCountsHandler - Contact Sensor - ${theName} - newValues: ${newValues}"
            state.contactMap.put(theName, newValues)
        }
    }
    if(contactEvent) { buildMaps("Contact Sensor") }
    
    if(logEnable) log.debug "In resetCountsHandler - Resetting Motion - dataValue: ${dataValue}"
    motionEvent.each { it ->
        String theName = it       
        theCounts = state.motionMap.get(it.displayName)
        if(theCounts) {     
            def (theDay,theWeek,theMonth,theYear) = theCounts.split(",")
            theZero = 0
            if(dataValue == "D") newValues = "${theZero},${theWeek},${theMonth},${theYear}"
            if(dataValue == "W") newValues = "${theZero},${theZero},${theMonth},${theYear}"
            if(dataValue == "M") newValues = "${theZero},${theZero},${theZero},${theYear}"
            if(dataValue == "Y") newValues = "${theZero},${theZero},${theZero},${theZero}"
            if(logEnable) log.debug "In resetCountsHandler - Motion Sensor - ${theName} - newValues: ${newValues}"
            state.motionMap.put(theName, newValues)
        }
    }
    if(motionEvent) { buildMaps("Motion Sensor") }
    
    if(logEnable) log.debug "In resetCountsHandler - Resetting Switch - dataValue: ${dataValue}"
    switchEvent.each { it ->
        String theName = it       
        theCounts = state.switchMap.get(it.displayName)
        if(theCounts) {     
            def (theDay,theWeek,theMonth,theYear) = theCounts.split(",")
            theZero = 0
            if(dataValue == "D") newValues = "${theZero},${theWeek},${theMonth},${theYear}"
            if(dataValue == "W") newValues = "${theZero},${theZero},${theMonth},${theYear}"
            if(dataValue == "M") newValues = "${theZero},${theZero},${theZero},${theYear}"
            if(dataValue == "Y") newValues = "${theZero},${theZero},${theZero},${theZero}"
            if(logEnable) log.debug "In resetCountsHandler - Switch - ${theName} - newValues: ${newValues}"
            state.switchMap.put(theName, newValues)
        }
    }
    if(switchEvent) { buildMaps("Switch") }
    
    if(logEnable) log.debug "In resetCountsHandler - Resetting Thermostat - dataValue: ${dataValue}"
    thermostatEvent.each { it ->
        String theName = it       
        theCounts = state.thermostatMap.get(it.displayName)
        if(theCounts) {     
            def (theDay,theWeek,theMonth,theYear) = theCounts.split(",")
            theZero = 0
            if(dataValue == "D") newValues = "${theZero},${theWeek},${theMonth},${theYear}"
            if(dataValue == "W") newValues = "${theZero},${theZero},${theMonth},${theYear}"
            if(dataValue == "M") newValues = "${theZero},${theZero},${theZero},${theYear}"
            if(dataValue == "Y") newValues = "${theZero},${theZero},${theZero},${theZero}"
            if(logEnable) log.debug "In resetCountsHandler - Thermostat - ${theName} - newValues: ${newValues}"
            state.thermostatMap.put(theName, newValues)
        }
    }
    if(thermostatEvent) { buildMaps("Thermostat") }
}

def buildMaps(data) {
    if(logEnable) log.debug "In buildMaps (${state.version}) - ${data}"
    
    if(data == "Contact Sensor") {
        reportMap = state.contactMap
    } else if(data == "Motion Sensor") {
        reportMap = state.motionMap
    } else if(data == "Switch") {
        reportMap = state.switchMap
    } else if(data == "Thermostat") {
        reportMap = state.thermostatMap
    }

    def tblhead = "<div style='overflow:auto;height:90%'><table style='width:100%;line-height:1.00;font-size:${fontSize}px;text-align:left'><tr><td><b>${data}</b><td><b>Day</b><td><b>Week</b><td><b>Month</b><td><b>Year</b>"

    def line = "" 
    def tbl = tblhead
    def tileCount = 1
    state.count = 0

    if(reportMap) {
        theMap = reportMap.sort()

        theMap.each { it ->
            String theName = it.key
            state.count = state.count + 1
            
            if(it.value) {
                if(logEnable) log.debug "In buildMaps - Sending ${it.value}"
                getNumbersHandler(it.value)
                def (theDay,theWeek,theMonth,theYear) = theTimers.split("::")

                line = "<tr><td>${theName}<td>${theDay}<td>${theWeek}<td>${theMonth}<td>${theYear}"

                totalLength = tbl.length() + line.length()
                if(logEnable) log.debug "In buildMaps - tbl Count: ${tbl.length()} - line Count: ${line.length()} - Total Count: ${totalLength}"

                if (totalLength < 1007) {
                    tbl += line
                } else {
                    tbl += "</table></div>"
                    if(logEnable) log.debug "${tbl}"
                    if(dataDevice) {
                        if(logEnable) log.debug "In buildMaps - Sending new data to Tiles (${tileCount})"
                        sending = "${tileCount}::${tbl}"
                        if(data == "Contact Sensor") dataDevice.sendContactMap(sending)
                        if(data == "Motion Sensor") dataDevice.sendMotionMap(sending)
                        if(data == "Switch") dataDevice.sendSwitchMap(sending)
                        if(data == "Thermostat") dataDevice.sendThermostatMap(sending)
                        tileCount = tileCount + 1
                    }
                    tbl = tblhead + line 
                }
            }
        }

        if (tbl != tblhead) {
            tbl += "</table></div>"
            if(logEnable) log.debug "${tbl}"
            if(dataDevice) {
                if(logEnable) log.debug "In buildMaps - Sending new data to Tiles (${tileCount})"
                sending = "${tileCount}::${tbl}"
                if(data == "Contact Sensor") dataDevice.sendContactMap(sending)
                if(data == "Motion Sensor") dataDevice.sendMotionMap(sending)
                if(data == "Switch") dataDevice.sendSwitchMap(sending)
                if(data == "Thermostat") dataDevice.sendThermostatMap(sending)
                tileCount = tileCount + 1
            }
        }

        for(x=tileCount;x<4;x++) {
            sending = "${x}::<div style='font-size:${fontSize}px'>No Data</div>"
            if(data == "Contact Sensor") dataDevice.sendContactMap(sending)
            if(data == "Motion Sensor") dataDevice.sendMotionMap(sending)
            if(data == "Switch") dataDevice.sendSwitchMap(sending)
            if(data == "Thermostat") dataDevice.sendThermostatMap(sending)
        }	
    } else {
        log.warn "In buildMaps - data: ${data} - reportMap: ${reportMap}"
    }
}

def removeOrphanedDevices() {
    if(logEnable) log.debug "In removeOrphanedDevices (${state.version})"
    state.newContactMap = [:]
    state.newMotionMap = [:]
    state.newSwitchMap = [:]
    state.newThermostatMap = [:]
    
    contactEvent.each { it ->
        String theName = it        
        theCounts = state.contactMap.get(it.displayName)
        if(theCounts) { state.newContactMap.put(theName, theCounts) }
    }
    state.contactMap = [:]
    state.contactMap = state.newContactMap.clone()
    if(state.contactMap) buildMaps("Contact Sensor")
    
    motionEvent.each { it ->
        String theName = it        
        theCounts = state.motionMap.get(it.displayName)
        if(theCounts) { state.newMotionMap.put(theName, theCounts) }
    }
    state.motionMap = [:]
    state.motionMap = state.newMotionMap.clone()
    if(state.motionMap) buildMaps("Motion Sensor")
    
    switchEvent.each { it ->
        String theName = it        
        theCounts = state.switchMap.get(it.displayName)
        if(theCounts) { state.newSwitchMap.put(theName, theCounts) }
    }
    state.switchMap = [:]
    state.switchMap = state.newSwitchMap.clone()
    if(state.switchMap) buildMaps("Switch")
    
    thermostatEvent.each { it ->
        String theName = it        
        theCounts = state.thermostatMap.get(it.displayName)
        if(theCounts) { state.newThermostatMap.put(theName, theCounts) }
    }
    state.thermostatMap = [:]
    state.thermostatMap = state.newThermostatMap.clone()
    if(state.thermostatMap) buildMaps("Thermostat")
    
    if(logEnable) log.debug "In removeOrphanedDevices - Maps Cleaned"
}

def getNumbersHandler(data) {
    if(logEnable) log.debug "In getNumbersHandler (${state.version}) - Received: ${data}"
       
    def (theDay,theWeek,theMonth,theYear) = data.split(",")
    if(logEnable) log.debug "In getNumbersHandler - Working with d: ${theDay} - w: ${theWeek} - m: ${theMonth} - y: ${theYear}"
    
    dDay = Math.round(theDay.toInteger() / 86400)
    dHrs = Math.round((theDay.toInteger() % 86400 ) / 3600)
    dMin = Math.round(((theDay.toInteger() % 86400 ) % 3600 ) / 60)
    dSec = Math.round(((theDay.toInteger() % 86400 ) % 3600 ) % 60)
    theDayTime = "${dHrs} H, ${dMin} M, ${dSec} S"
    
    wDay = Math.round(theWeek.toInteger() / 86400)
    wHrs = Math.round((theWeek.toInteger() % 86400 ) / 3600)
    wMin = Math.round(((theWeek.toInteger() % 86400 ) % 3600 ) / 60)
    wSec = Math.round(((theWeek.toInteger() % 86400 ) % 3600 ) % 60)    
    theWeekTime = "${wDay} D, ${wHrs} H, ${wMin} M, ${wSec} S"
    
    mDay = Math.round(theMonth.toInteger() / 86400)
    mHrs = Math.round((theMonth.toInteger() % 86400 ) / 3600)
    mMin = Math.round(((theMonth.toInteger() % 86400 ) % 3600 ) / 60)
    mSec = Math.round(((theMonth.toInteger() % 86400 ) % 3600 ) % 60)   
    theMonthTime = "${mDay} D, ${mHrs} H, ${mMin} M, ${mSec} S"
    
    yDay = Math.round(theYear.toInteger() / 86400)
    yHrs = Math.round((theYear.toInteger() % 86400 ) / 3600)
    yMin = Math.round(((theYear.toInteger() % 86400 ) % 3600 ) / 60)
    ySec = Math.round(((theYear.toInteger() % 86400 ) % 3600 ) % 60)  
    theYearTime = "${yDay} D, ${yHrs} H, ${yMin} M, ${ySec} S"
    
    theTimers = "${theDayTime}::${theWeekTime}::${theMonthTime}::${theYearTime}"
    return theTimers
}

def createDataChildDevice() {    
    if(logEnable) log.debug "In createDataChildDevice (${state.version})"
    statusMessageD = ""
    if(!getChildDevice(dataName)) {
        if(logEnable) log.debug "In createDataChildDevice - Child device not found - Creating device: ${dataName}"
        try {
            addChildDevice("BPTWorld", "Abacus Time Traveler Driver", dataName, 1234, ["name": "${dataName}", isComponent: false])
            if(logEnable) log.debug "In createDataChildDevice - Child device has been created! (${dataName})"
            statusMessageD = "<b>Device has been been created. (${dataName})</b>"
        } catch (e) { if(logEnable) log.debug "Abacus was unable to create the data device - ${e}" }
    } else {
        statusMessageD = "<b>Device Name (${dataName}) already exists.</b>"
    }
    return statusMessageD
}
    
// ********** Normal Stuff **********

def logsOff() {
    log.info "${app.label} - Debug logging auto disabled"
    app?.updateSetting("logEnable",[value:"false",type:"bool"])
}

def checkEnableHandler() {
    state.eSwitch = false
    if(disableSwitch) { 
        if(logEnable) log.debug "In checkEnableHandler - disableSwitch: ${disableSwitch}"
        disableSwitch.each { it ->
            state.eSwitch = it.currentValue("switch")
            if(state.eSwitch == "on") { state.eSwitch = true }
        }
    }
}

def setDefaults() {
	if(logEnable == null){logEnable = false}
    if(state.contactMap == null) state.contactMap = [:]
    if(state.motionMap == null) state.motionMap = [:]
    if(state.switchMap == null) state.switchMap = [:]
    if(state.thermostatMap == null) state.thermostatMap = [:]
}

def getImage(type) {					// Modified from @Stephack Code
    def loc = "<img src=https://raw.githubusercontent.com/bptworld/Hubitat/master/resources/images/"
    if(type == "Blank") return "${loc}blank.png height=40 width=5}>"
    if(type == "checkMarkGreen") return "${loc}checkMarkGreen2.png height=30 width=30>"
    if(type == "optionsGreen") return "${loc}options-green.png height=30 width=30>"
    if(type == "optionsRed") return "${loc}options-red.png height=30 width=30>"
    if(type == "instructions") return "${loc}instructions.png height=30 width=30>"
    if(type == "logo") return "${loc}logo.png height=60>"
    if(type == "powerGreen") return "${loc}button-power-green.png height=10>"
    if(type == "powerRed") return "${loc}button-power-red.png height=10>"
}

def getFormat(type, myText="") {			// Modified from @Stephack Code   
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
    if(type == "line") return "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

def display() {
    setVersion()
    getHeaderAndFooter()
    theName = app.label
    if(theName == null || theName == "") theName = "New Child App"
    section (getFormat("title", "${getImage("logo")}" + " ${state.name} - ${theName}")) {
        paragraph "${state.headerMessage}"
		paragraph getFormat("line")
	}
}

def display2() {
	section() {
		paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center;font-size:20px;font-weight:bold'>${state.name} - ${state.version}</div>"
        paragraph "${state.footerMessage}"
	}       
}

def getHeaderAndFooter() {
    timeSinceNewHeaders()   
    if(state.totalHours > 4) {
        //if(logEnable) log.debug "In getHeaderAndFooter (${state.version})"
        def params = [
            uri: "https://raw.githubusercontent.com/bptworld/Hubitat/master/info.json",
            requestContentType: "application/json",
            contentType: "application/json",
            timeout: 30
        ]

        try {
            def result = null
            httpGet(params) { resp ->
                state.headerMessage = resp.data.headerMessage
                state.footerMessage = resp.data.footerMessage
            }
        }
        catch (e) { }
    }
    if(state.headerMessage == null) state.headerMessage = "<div style='color:#1A77C9'><a href='https://github.com/bptworld/Hubitat' target='_blank'>BPTWorld Apps and Drivers</a></div>"
    if(state.footerMessage == null) state.footerMessage = "<div style='color:#1A77C9;text-align:center'>BPTWorld Apps and Drivers<br><a href='https://github.com/bptworld/Hubitat' target='_blank'>Donations are never necessary but always appreciated!</a><br><a href='https://paypal.me/bptworld' target='_blank'><b>Paypal</b></a></div>"
}

def timeSinceNewHeaders() { 
    if(state.previous == null) { 
        prev = new Date()
    } else {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        prev = dateFormat.parse("${state.previous}".replace("+00:00","+0000"))
    }
    def now = new Date()
    use(TimeCategory) {       
        state.dur = now - prev
        state.days = state.dur.days
        state.hours = state.dur.hours
        state.totalHours = (state.days * 24) + state.hours
    }
    state.previous = now
    //if(logEnable) log.warn "In checkHoursSince - totalHours: ${state.totalHours}"
}
