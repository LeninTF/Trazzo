module.exports = function (config) {
    config.set({
        basePath: '',
        frameworks: ['jasmine', '@angular-devkit/build-angular'],
        plugins: [
            require('karma-jasmine'),
            require('karma-chrome-launcher'),
            require('karma-jasmine-html-reporter'),
            require('karma-coverage'),
            require('@angular-devkit/build-angular/plugins/karma')
        ],
        client: {
            jasmine: {
                random: false
            },
            clearContext: false
        },
        coverageReporter: {
            dir: require('path').join(__dirname, './coverage'),
            subdir: '.',
            reporters: [
                { type: 'html' },
                { type: 'lcovonly' },
                { type: 'text-summary' }
            ]
        },
        reporters: ['progress', 'kjhtml'],
        port: 9876,
        colors: true,
        logLevel: config.LOG_INFO,
        autoWatch: false,
        browsers: ['ChromeHeadless'],
        singleRun: true,
        restartOnFileChange: false
    });
};