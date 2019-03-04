const https = require('https');
const Koa = require('koa');
const KoaRouter = require('koa-router');
const fs = require('fs');
const path = require('path');
const request = require('request-promise');

const credentials = {
    "id": "<CLIENT_ID>", // TODO 12345678-1234-1234-1234-123456123456
    "secret": "<CLIENT_SECRET>" // TODO XxXXxxxXXxxXxxXXxxxxxxxxXXXXx-xXxXxX
};

const key = fs.readFileSync('<PATH_TO_YOUR_KEY>'); // TODO ./key.pem
const cert = fs.readFileSync('<PATH_TO_YOUR_CERT>'); // TODO ./cert.pem

const apiUrl = '<YOUR_PURECLOUD_DOMAIN>' // TODO mypurecloud.com, mypurecloud.ie, mypurecloud.de, etc
const listenPort = 443;

let apiCredentials;

async function createServer () {
    const app = new Koa();
    const router = new KoaRouter();
    app.use(setupRoutes(router));

    const server = https.createServer({key, cert}, app.callback());
    server.listen(listenPort);
    console.log(`listening ${listenPort}`);
}


async function getUserInformation(clientToken) {
    // TODO ensure user is logged in, e.g. validate a client token and retrieve user details
    let userEmail;
    if (clientToken) {
        userEmail = 'joe.customer@example.com'
    } else {
        throw new Error('No client token provided');
    }

    return {
        // TODO add user information that you want to pass to the agent eg account-id, full-name
        authorized: true,
        userEmail
    };
}

function setupRoutes (router) {
    router.get('/get-jwt', async (ctx) => {
        let userInformation;
        // TODO data used for authentication comes in on the request headers
        const clientToken = ctx.get('Client-Token');
        try {
            userInformation = await getUserInformation(clientToken); // TODO ensure the user is authenticated
        } catch (error) {
            ctx.throw(400, error.message);
            return;
        }

        console.log('/get-jwt request');
        const {jwt} = await getSignature({userInformation});
        ctx.body = {jwt};
        ctx.set('Access-Control-Allow-Origin', '<YOUR_WEB_APP_ORIGIN>'); // TODO https://example.com
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
        return JSON.parse(response);
    } catch (error) {
        console.error({error: error});
    }
}

async function getSignature ({userInformation}) {
    const {token_type, access_token} = apiCredentials;
    const response = await request({
        url: `https://api.${apiUrl}/api/v2/signeddata`,
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `${token_type} ${access_token}`
        },
        body: JSON.stringify(userInformation)
    });
    return JSON.parse(response);
}

(async () => {
    apiCredentials = await getApiCredentials();
    await createServer();
})();
