/* Services */

angular.module('myApp.services', []).
  value('version', '0.1')

  .factory('WS', ['$rootScope', '$http', function($rootScope, $http) {
    var wsUrl = angular.element(document.querySelector('#ws-url')).val();
    /*
    var ws = new WebSocket(wsUrl);
    ws.onmessage = function(receivedData) {
      $rootScope.$apply(function() {
        var message = JSON.parse(receivedData.data);
        //console.log("msg received", message);
        if (message.msgType === "txlog") {
          Service.transactionLog.incoming = message.incoming;
          Service.transactionLog.outgoing = message.outgoing;
        }
      });
    }; 
    ws.onopen = function() {
      console.log("websocket is open now");
    };
    ws.onclose = function() { 
      console.log("The WebSocket just closed!"); 
    };    
    */

    var Service = {
      transactionLog: {
        incoming: [],
        outgoing: []
      },

      getTransactions: function() {
        var getTxUrl = angular.element(document.querySelector('#get-transactions-url')).val();
        $http.get(getTxUrl).
          success(function(data, status, headers, config) {
            console.log("success in sendMsg", data);

            Service.transactionLog.incoming = data.incoming;
            Service.transactionLog.outgoing = data.outgoing;            
            // this callback will be called asynchronously
            // when the response is available
          }).
          error(function(data, status, headers, config) {
            console.log("error in sendMsg", data);
            // called asynchronously if an error occurs
            // or server returns response with an error status.
          });
      },

      sendMsg: function(msg) {
        var playLotteryUrl = angular.element(document.querySelector('#play-lottery-url')).val();
        $http.post(playLotteryUrl, msg).
          success(function(data, status, headers, config) {
            console.log("success in sendMsg");
            // this callback will be called asynchronously
            // when the response is available
          }).
          error(function(data, status, headers, config) {
            console.log("error in sendMsg", data);
            // called asynchronously if an error occurs
            // or server returns response with an error status.
          });
      }
    };
    return Service;
  }]);