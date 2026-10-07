# E-journey

## Gateway:
```
### Authentication and role based filtering
### CORS config
...
```

## user service:

```
# Authentication:

/v1/api/ ==> /auth ===> /regiter
                   ===> /login

# User operations:
         ==> /user ===> /profile : GET, DELETE, PUT

# Admin options in user:
         ==> /admin ===> /users : GET, DELETE
         ==> /admin ===> /travels : GET, DELETE
         ==> /admin ===> /payments : GET, DELETE
```
## travel service:

```
# CRUD on travel:

/v1/api/ ==> /travel:id ===> GET, DELETE, POST, PUT
/api/v1/places/public ==> GET (public endpoint to see places but booking requires login/register)
```

## payment service:
```
## For late .. (paypal, Stripe)

```


