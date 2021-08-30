db = db.getSiblingDB('dashboarddb'); db.createUser({  user: "admin", pwd: "admin", roles: [{ role: "userAdminAnyDatabase", db: "admin" }]});
