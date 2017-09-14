config.$inject = ['$locationProvider', '$routeProvider'];
export default function config($locationProvider, $routeProvider) {
    $locationProvider.html5Mode(true);
    $routeProvider
        .when('/', {
            template: require('./instagram/query/ig_query.html'),
            controller: 'IgQueryController as vm'
        })
        .when('/@:username', {
            template: require('./instagram/pack_conf/pack_conf.html'),
            controller: 'PackConfController as vm'
        })
        .when('/p/:packId', {
            template: require('./common/pack/pack.html'),
            controller: 'PackController as vm',
            resolve: {
                pack: resolvePack
            }
        })
        .when('/about', {
            template: require('./templates/about.html')
        })
        .otherwise({
            template: require('./templates/404.html')
        });
}

resolvePack.$inject = ['$route', 'api'];
function resolvePack($route, api) {
    return api.getPack($route.current.params.packId).then(pack => pack);
}
