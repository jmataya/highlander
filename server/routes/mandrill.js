import makeRouter from 'koa-router';
import { Mandrill } from 'mandrill-api/mandrill';

function *sendMessage(mandrillClient, params) {
  return new Promise((resolve, reject) => {
    mandrillClient.messages.send(params, result => {
      resolve(result);
      console.log(`RESOLVED : ${result}`);
    }, error => {
      const err = new Error(error.message || error);
      reject(err);
      console.log(`REJECTED : ${error.message}`);
    });
  });
}

export default function mandrillRouter(apiKey) {
  const mandrillClient = new Mandrill(apiKey);

  const router = makeRouter()
    .post('/api/node/mandrill', function*() {
      const { message } = this.request.body;

      const async = false;

      console.log('WE ARE SENDING!');
      yield sendMessage(mandrillClient, {
        message,
        async
      });

      this.body = {};
    });

  return router.routes();
}
