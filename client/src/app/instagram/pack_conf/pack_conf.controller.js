export default class PackConfController {
    constructor($routeParams, api, location, user) {
        if (user == null) {
            location.openSearch($routeParams.username);
            return;
        }
        this.user = user;
        this.$routeParams = $routeParams;
        this.api = api;
        this.location = location;
        this.settings = {
            username: user.username,
            includeImages: true,
            includeVideos: true,
            fileNamePattern: 'timestamp'
        };
        this.filenameExamples = new Map([
            ['id',        {img: '1756...364.jpg',           vid: '4606...591.mp4'}],
            ['index',     {img: '1.jpg',                    vid: '2.mp4'}],
            ['utctime',   {img: '2017-02-25T15:36:59Z.jpg', vid: '2016-05-10T14:24:20Z.mp4'}],
            ['timestamp', {img: '1497899933.jpg',           vid: '1497788183.mp4'}]
        ]);
        this.processing = false; // waiting for the response of post create pack
        this.user.instagramPageLink = 'https://www.instagram.com/' + this.user.username + '/';
    }

    createPackClick() {
        this.processing = true;
        this.api.createPack(this.settings).then(pack => {
            if (pack != null) this.location.openPack(pack.id);
            this.processing = false;
        });
    }

    searchAnotherUser() {
        this.location.openSearch();
    }

    username() {
        if (this.user.username.length > 20)
            return this.user.username.substring(0, 20) + '..';
        else
            return this.user.username;
    }

    ready() {
        return !this.processing && this.user.count > 0 && (this.settings.includeVideos || this.settings.includeImages);
    }

    filenameExample() {
        let example = '';
        if (this.settings.includeImages)
            example += this.filenameExamples.get(this.settings.fileNamePattern).img + ', ';
        if (this.settings.includeVideos)
            example += this.filenameExamples.get(this.settings.fileNamePattern).vid;
        if (example === '') example = 'no files included';
        return example;
    }
}

PackConfController.$inject = ['$routeParams', 'api', 'location', 'user'];
