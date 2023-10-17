/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import java.util.Properties;

/**
 * @author ssydoruk
 */
@FunctionalInterface
public interface ICalculatedFields {

    Properties calc(Properties m_fieldsAll);
}
