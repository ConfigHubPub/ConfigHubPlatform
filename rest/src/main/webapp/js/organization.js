angular
    .module('configHub.organization', [
        'configHub.organization.create'
    ])

    .config(['$stateProvider', function ($stateProvider)
    {
        $stateProvider

            .state('createOrganization', {
                url: '/organization/create',
                templateUrl: 'organization/create.html',
                pageTitle: 'New Organization',
                data: {
                    requireLogin: true
                }
            })

        ;
    }])

    ;

