const fetch = require('node-fetch');

const data = {
    payment_type_id: 1, 
    note: "test", 
    required_note: "CHOXEMHANGKHONGTHU", 
    to_name: "Test Name", 
    to_phone: "0987654321", 
    to_address: "123 Street", 
    to_ward_code: "21211", 
    to_district_id: 1452, 
    cod_amount: 0, 
    weight: 200, 
    length: 20, 
    width: 15, 
    height: 10, 
    service_type_id: 2, 
    items: [{name:"Book 1",code:"B1",quantity:1,price:100000,weight:200}]
};

fetch("https://dev-online-gateway.ghn.vn/shiip/public-api/v2/shipping-order/create", {
    method: 'POST',
    headers: {
        'Token': 'dffec2e1-6725-11f1-a973-aee5264794df',
        'ShopId': '200948',
        'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
}).then(r => r.json()).then(console.log).catch(console.error);
