/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.myutils.logbrowser.inquirer;

import Utils.Pair;
import com.myutils.logbrowser.inquirer.gui.TabResultDataModel;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;

/**
 * @author Stepan
 */
public abstract class IAggregateAggregate extends IAggregateQuery {

    @Override
    void Print(PrintStreams ps) throws Exception {
        if (resultQuery != null) {
            String lastTs = null;
            String ts;
            Pair<Color, Color> co = null;
            int ColorIdx = 0;
            resultQuery.Execute();
            String[] fieldNames = resultQuery.getFieldNames();

            ps.println(StringUtils.join(resultQuery.getFieldNames(), ","));
            String[] GetNextStringArray;
            while ((GetNextStringArray = resultQuery.GetNextStringArray()) != null) {
                ts = GetNextStringArray[0];
                ps.println(StringUtils.join(GetNextStringArray, ","));
                for (int i = 0; i < fieldNames.length; i++) {
                    ps.addField(fieldNames[i], GetNextStringArray[i]);
                }
                if (!ts.equals(lastTs)) {
                    lastTs = ts;
                    co = TabResultDataModel.getColorIdx((ColorIdx++) % 2);
                }
                ps.currentRow().setCellColor(co);
                ps.newRow();
            }
            resultQuery.Reset();
        }
    }

}
