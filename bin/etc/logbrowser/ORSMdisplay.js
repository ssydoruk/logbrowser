switch (RECORD.getField("metric")) {
  
    case "log": {
      var s = RECORD.getBytes();
      PRINTOUT.lang="json";

      PRINTOUT.fullMessage=s.replace(/\<\/br\>/g, '\n').replace(/\'( label=)/, '\'\n\n$1');
      
      if ((m = s.match(/IN THE ATTACH KVPs: (\{[^\}]+\})/)) != undefined) {
        PRINTOUT.detailsMessage=JSON.stringify(JSON.parse(m[1]), undefined, 4);
      }
      else if ((m = s.match(/fetch done\"({.+})\"/)) != undefined) {
        var s = m[1];
        PRINTOUT.detailsMessage= s.replace(/\\u000a/g, '\n').replace(/\\\"/g, '\"');
      }
      else if ((m = s.match(/application thread \((.+)\)\'/)) != undefined) {
//        var s = m[1].replace(/\\n/g, '\n').replace(/\\\"/g, '\"').replace(/\\\\\"/g, '\"');
        var s = m[1].replace(/\\n/g, '\n').replace(/\\\"/g, '\"');
        // console.log(s);
        try{
          PRINTOUT.detailsMessage='-->'+JSON.stringify(JSON.parse(s.replace(/(\w+):/g, '\"$1\":')), undefined, 4);
        }
        catch (e){
          console.log('error parsing: '+e.message);
          PRINTOUT.detailsMessage='++>'+s;
        }
      }
      else if ((m = s.match(/IN THE BUSINESS RULE.+vRequest: (.+\})/)) != undefined) {
        var el=m[1];
        try{
          PRINTOUT.detailsMessage='-->'+JSON.stringify(JSON.parse(el), undefined, 4);
        }
        catch (e){
          console.log('error parsing: '+e.message);
          PRINTOUT.detailsMessage='++>'+el;
        }

      }
      else if ((m = s.match(/Rule Results :[^\{]+(\{.+\}).+_data.data array:[^\{]+(\{.+\})/)) != undefined) {
        var el=m[1];
        var dd=m[2];
        try{
          PRINTOUT.detailsMessage='Rule Results :\n'+JSON.stringify(JSON.parse(el), undefined, 4)+
          '\n_data.data array:\n'+JSON.stringify(JSON.parse(dd), undefined, 4);
        }
        catch (e){
          console.log('error parsing: '+e.message);
          PRINTOUT.detailsMessage='++>'+el;
          PRINTOUT.detailsMessage='++>Rule Results :\n'+el+
          '\n_data.data array:\n'+dd;
        }

      }
      else if ((m = s.match(/_data.data:[^\{]+(\{.+\}).+RoutingParameters:[^\{]+(\{.+\})/)) != undefined) {
        var dt=m[1];
        var rp=m[2];
        try{
          PRINTOUT.detailsMessage='_data.data\n'+JSON.stringify(JSON.parse(dt), undefined, 4)
          + '\nRoutingParameters\n'+JSON.stringify(JSON.parse(rp), undefined, 4)
        }
        catch (e){
          console.log('error parsing: '+e.message);
          PRINTOUT.detailsMessage='++>'+
          '_data.data\n'+dt
          + '\nRoutingParameters\n'+rp;
        }

      }

      else {
        // PRINTOUT.detailsMessage=JSON.stringify({"cc":"bb", "key1":"val1"});
      }

      break;
    }
  
    default:
      break;
  }
