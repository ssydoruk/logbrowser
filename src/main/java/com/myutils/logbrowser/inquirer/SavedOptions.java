/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.myutils.logbrowser.inquirer;

import com.google.gson.internal.LinkedTreeMap;
import java.util.HashMap;
import org.json.JSONObject;

/**
 *
 * @author ssydo
 */
class SavedOptions extends HashMap<String, JSONObject> {



    public JSONObject getTree(DialogItem[] dialogItem) {
        for (DialogItem item : dialogItem) {
//            LinkedTreeMap obj = (LinkedTreeMap) savedOptions.get(dialogItem.toString());
//            if (obj != null) {
//                LinkedTreeMap map = (LinkedTreeMap) obj.get("map");
//                boolean enabled = true;
//                if (map != null) {
//                    Object obj1 = map.get("enabled");
//                    if (obj1 != null) {
//                        enabled = (Boolean) obj1;
//                    }
//                }
            return get(item.toString());

        }
        return null;
    }
}
