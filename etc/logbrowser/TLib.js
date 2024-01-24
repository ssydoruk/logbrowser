
// uncomment when debuging standalone
// const dbg=require('./DEBUG.js'); var RECORD=dbg.RECORD;  var FIELDS = dbg.FIELDS;
var strategyName = undefined;
var strategyDBID = undefined;

for (var s of RECORD.getBytes().split('\n')) {
  if ((m = s.match(/AttributePrivateMsgID\s*(\d*)$/i)) != undefined) {
    FIELDS.put("PrivateMsgID", {
      value: "msgid=" + m[1],
      hidden: true
    });
  } else if ((m = s.match(/AttributeLocation\s(\S*)$/i)) != undefined) {
    FIELDS.put("location", {
      value: "loc=" + m[1],
      hidden: true
    });
  } else if ((m = s.match(/AttributeCollectedDigits\s'(\S*)'$/i)) != undefined) {
    FIELDS.put("digits", {
      value: "digits=" + m[1],
      hidden: true
    });
  } else if ((m = s.match(/Session'\s'(\S*)'/i)) != undefined) {
    FIELDS.put("sid", "sid=" + m[1]);
  } else if ((m = s.match(/RStrategyName'\s'(\S*)'/i)) != undefined) {
    strategyName = m[1];
  } else if ((m = s.match(/RStrategyDBID'\s'(\S*)'/i)) != undefined) {
    strategyDBID = m[1];
  } else if ((m = s.match(/(from ISCC):/)) != undefined) {
    FIELDS.put("source", m[1]);
  } else if ((m = s.match(/AttributeCallState\s(\d*)$/)) != undefined) {
    try {
      var i = parseInt(m[1]);
      if (
        i > 0 ||
        !RECORD.getField("name").toLowerCase().startsWith("request")
      ) {
        FIELDS.put("callParams", "state: " + RECORD.constToStr("CallState", i));
      }
    } catch (e) { }
  }
}
if (strategyName || strategyDBID) {
  FIELDS.put('Strategy',  ((strategyName) ? strategyName : 'DBID:') + '(' + ((strategyDBID) ? strategyDBID : '') + ')');
}
// console.log(FIELDS.entrySet());