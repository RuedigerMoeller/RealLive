// version 2 of js RealLive facade as callback/connection management moved to kontraktor

var RL_UPDATE    = 0;
var RL_ADD       = 1;
var RL_REMOVE    = 2;
var RL_OPERATION = 3;
var RL_SNAPSHOT_DONE = 4;
var RL_ERROR = 5;

var RealLive = new function() {
};