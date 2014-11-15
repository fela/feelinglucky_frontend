/* Services */

angular.module('myApp.services', []).
  value('version', '0.1')

  .factory('WS', ['$rootScope', function($rootScope) {
    var wsUrl = angular.element(document.querySelector('#ws-url')).val();
    var ws = new WebSocket(wsUrl);
    ws.onmessage = function(receivedData) {
      $rootScope.$apply(function() {
        var message = JSON.parse(receivedData.data);
        //console.log("msg received", message);
        if (message.msgType === "txlog") {
          Service.transactionLog.txs = message.txs;
        }
      });
    }; 
    ws.onopen = function() {
      console.log("websocket is open now");
    };
    ws.onclose = function() { 
      console.log("The WebSocket just closed!"); 
    };    

    var Service = {
      ws: ws,
      transactionLog: {
        txs: []
      },

      sendMsg: function(msg) {
        //console.log("sending", msg);
        this.ws.send(JSON.stringify(msg));
      }
    };
    return Service;
  }]);