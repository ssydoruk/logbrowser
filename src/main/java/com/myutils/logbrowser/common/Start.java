/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.common;

import com.myutils.logbrowser.indexer.Main;
import com.myutils.logbrowser.inquirer.inquirer;
import org.apache.commons.lang3.ArrayUtils;

/**
 * @author stepan_sydoruk
 */
public class Start {

    static EnvStart ee;

    static void helpExit() {
        System.out.println("\nUsage: <Application> <inquirer|indexer> <app parameters>\n");
        System.exit(1);
    }

    public static void main(String[] args) throws Exception {
        if (ArrayUtils.isEmpty(args)) {
            helpExit();
        }
        if (args[0].equalsIgnoreCase("inquirer")) {
            inquirer.main(ArrayUtils.subarray(args, 1, args.length));
        } else if (args[0].equalsIgnoreCase("indexer")) {
            Main.main(ArrayUtils.subarray(args, 1, args.length));
        } else {
            helpExit();
        }
    }
}
