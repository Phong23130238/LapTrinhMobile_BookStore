const https = require('https');

const options = {
  hostname: 'online-gateway.ghn.vn',
  port: 443,
  path: '/shiip/public-api/master-data/province',
  method: 'GET',
  headers: {
    'Token': 'dffec2e1-6725-11f1-a973-aee5264794df'
  }
};

const req = https.request(options, res => {
  console.log(`statusCode: ${res.statusCode}`);
  let data = '';
  res.on('data', d => {
    data += d;
  });
  res.on('end', () => {
      console.log(data.substring(0, 100));
  });
});

req.on('error', error => {
  console.error(error);
});

req.end();
