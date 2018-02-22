/* eslint-env node */
/* eslint-disable no-console */
const https = require('https');
const Koa = require('koa');
const KoaRouter = require('koa-router');
const koaStatic = require('koa-static');
const fs = require('fs');
const path = require('path');
const request = require('request-promise');
const credentials = require('./oauth.credentials');
let apiCredentials;

const key = fs.readFileSync(path.join(__dirname, './key.pem'));
const cert = fs.readFileSync(path.join(__dirname, './cert.pem'));

let [apiUrl, listenPort] = process.argv.slice(2);
listenPort = listenPort || 8447;
apiUrl = apiUrl || 'mypurecloud.com';

async function createServer () {
    const app = new Koa();
    const router = new KoaRouter();
    app.use(setupRoutes(router));
    app.use(koaStatic(path.join(__dirname, 'static_files')));

    const server = https.createServer({key, cert}, app.callback());
    server.listen(listenPort);
    console.log(`listening ${listenPort}`);
}

function setupRoutes (router) {
    router.get('/get-jwt', async (ctx) => {
        console.log('/get-jwt');
        const {jwt} = await getSignature({apiUrl});
        ctx.body = {jwt};
    });
    return router.routes();
}

async function getApiCredentials () {
    try {
        const response = await request({
            method: 'POST',
            form: {grant_type:'client_credentials'},
            url: `https://login.${apiUrl}/oauth/token`
        }).auth(credentials.id, credentials.secret);
        console.log(response);
        return JSON.parse(response);
    } catch (error) {
        console.error({error: error});
    }
}

async function getSignature () {
    const {token_type, access_token} = apiCredentials;
    const response = await request({
        url: `https://api.${apiUrl}/api/v2/signeddata`,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `${token_type} ${access_token}`
        },
        body: JSON.stringify({
            authorized: true,
            date: Date.now()
        })
    });
    console.log(response);
    return JSON.parse(response);
}

(async () => {
    apiCredentials = await getApiCredentials();
    await createServer();
})();
