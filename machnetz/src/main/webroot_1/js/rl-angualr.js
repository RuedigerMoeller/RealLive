angular.module('rl-angular', []).
    config(function(injectables) { // provider-injector
        // This is an example of config block.
        // You can have as many of these as you want.
        // You can only inject Providers (not instances)
        // into config blocks.
    }).
    run(function(injectables) { // instance-injector
        // This is an example of a run block.
        // You can have as many of these as you want.
        // You can only inject instances (not Providers)
        // into run blocks
    });