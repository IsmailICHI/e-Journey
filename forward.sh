#Enter the VM
# port forwording to access the app from the VM through the clcuster : 
# gateway forward
sudo kubectl -n travel-plan port-forward service/gateway 18090:8090
Forwarding from 127.0.0.1:18090 -> 8090

# autoscalling checkout 
sudo kubectl get hpa -n travel-plan -w


# requests (change the 20 with the number of request desired per second)
for i in $(seq 1 20); do   curl -sS -o /dev/null     -w "Request $i: HTTP %{http_code} | %{time_total}s\n"     http://127.0.0.1:18090/actuator/health; done
