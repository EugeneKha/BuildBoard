/// <reference path='../_all.ts' />
module buildBoard {
    'use strict';

    export interface IBranchesScope extends ng.IScope {
        branches:Branch[];
        users:User[];

        allBranches:Branch[];


        countBy(userFilter:string, branchFilter:string):number;

        loading:boolean;

        userFilter:any;
        branchesFilter:any;

        loginToGithub(url:string):void;
        hideGithubLogin:boolean;
    }

    export class BranchesController {
        public static $inject = [
            '$scope',
            '$state',
            '$window',
            LoggedUserService.NAME,
            BranchesService.NAME
        ];

        constructor(private $scope:IBranchesScope, $state:ng.ui.IStateService, $window:ng.IWindowService, private loggedUserService:LoggedUserService, branchesService:BranchesService) {
            this.$scope.loading = true;

            this.$scope.userFilter = $state.params['user'] || 'all';
            this.$scope.branchesFilter = $state.params['branch'] || 'all';

            var lastBuildsPromise = branchesService.allBranchesWithLastBuilds;
            branchesService.allBranches.then(branches => {
                lastBuildsPromise.then(lastBuilds => this.lastBuildsCallback(branches, lastBuilds));
                this.branchesCallback(branches);
            }).then(x=> {
                this.$scope.loading = false;
            });

            this.$scope.loginToGithub = url=> {
                var otherWindow = $window.open(url, "", "menubar=no,location=yes,resizable=yes,scrollbars=yes,status=no");
                otherWindow.onunload = () => {
                    this.$scope.hideGithubLogin = true;
                    this.$scope.$apply();
                }
            }
        }

        private branchesCallback(branches:Branch[]) {
            var usersAndBranches = _.chain(branches)
                .filter(branch=>!!branch.entity)
                .map((branch:Branch) =>
                    _.map(branch.entity.assignments, user=> {
                        return {user: user, branch: branch};
                    }))
                .flatten()
                .value();

            var counts = _.countBy(usersAndBranches, userAndBranch=>userAndBranch.user.userId);

            var users = _.chain(usersAndBranches)
                .unique(false, pair=>pair.user.userId)
                .pluck('user')
                .value();

            _.forEach(users, user=> {
                user.count = counts[user.userId];
            });

            this.$scope.users = users;
            this.$scope.allBranches = branches;
            this.$scope.branches = this.filter(branches, this.$scope.userFilter, this.$scope.branchesFilter);
            this.$scope.countBy = (userFilter:string, branchesFilter:string)=>this.filter(branches, userFilter || this.$scope.userFilter, branchesFilter || "all").length;
        }

        lastBuildsCallback(branches:Branch[], builds:IMap<Build>) {
            _.each(branches, branch => {
                var build = builds[branch.name.toLowerCase()];
                if (build) {
                    if ((!branch.lastBuild || branch.lastBuild.timeStamp < build.timeStamp)) {
                        branch.lastBuild = build;
                    }
                }
                else {
                    branch.lastBuild = null;
                }
            });
        }

        filter(list:Branch[], userFilter:string, branchFilter:string):Branch[] {
            var userPredicate;

            var userId = userFilter == "my" ? this.loggedUserService.getLoggedUser().userId : parseInt(userFilter, 10);
            if (!isNaN(userId)) {
                userPredicate = branch=>branch.entity && _.any(branch.entity.assignments, assignment=>assignment.userId == userId);
            }
            else {
                userPredicate = branch=>true;
            }

            var branchPredicate;
            switch (branchFilter) {
                case "entity":
                    branchPredicate = branch=>branch.entity;
                    break;

                case "closed":
                    branchPredicate = (branch:Branch)=>branch.entity && branch.entity.state.isFinal;
                    break;

                case "special":
                    branchPredicate = (branch:Branch)=> branch.name.indexOf("release") == 0 ||
                        branch.name.indexOf("hotfix") == 0 ||
                        branch.name.indexOf("vs") == 0 ||
                        branch.name == "develop" ||
                        branch.name == "master";
                    break;
                default:
                    branchPredicate = branch=>true;
                    break;
            }

            return _.filter(list, branch=>userPredicate(branch) && branchPredicate(branch));
        }
    }
}
