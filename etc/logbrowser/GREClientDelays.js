
// uncomment when debuging standalone
// const dbg=require('./DEBUG.js'); var RECORD=dbg.RECORD;  var FIELDS = dbg.FIELDS;

for (var s of RECORD.getBytes().split('\n')) {
    if ((m = s.match(/^\s'Queue.+= \"(.+)\"$/i)) != undefined) {
        FIELDS.put("queue", m[1]);
    }
    else if ((m = s.match(/^\s{2,}'RequestorAppName'.+= \"(.+)\"$/i)) != undefined) {
        FIELDS.put("RequestorAppName", m[1]);
    }
    else if ((m = s.match(/^\s{2,}'RequestorClientId'.+= \"(.+)\"$/i)) != undefined) {
        FIELDS.put("RequestorClientId", m[1]);
    }
}
// console.log(FIELDS.entrySet());