
// uncomment when debuging standalone
// const dbg=require('./DEBUG.js'); var RECORD=dbg.RECORD;  var FIELDS = dbg.FIELDS;

for (var s of RECORD.getBytes().split('\n')) {
    if ((m = s.match(/attr_strategy_id.+= \"(.+)\"$/i)) != undefined) {
        FIELDS.put("strategy1", m[1]);
    }
    else if ((m = s.match(/Session.+=\s\"([^\"]+)/i)) != undefined) {
        FIELDS.put("sid", "sid=" + m[1]);
    }
}
// console.log(FIELDS.entrySet());