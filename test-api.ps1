# Test API endpoints
$baseUrl = "http://localhost:8080/api"
$testEmail = "test@example.com" # Change this to your email address

Write-Host "1. Registering notification request..."
$body = @{
    email = $testEmail
    movieName = "Avengers: Secret Wars"
    location = "New York"
    startDate = (Get-Date).ToString("yyyy-MM-dd")
    endDate = (Get-Date).AddDays(30).ToString("yyyy-MM-dd")
} | ConvertTo-Json

Invoke-RestMethod -Uri "$baseUrl/v1/notifications/register" `
    -Method Post `
    -Body $body `
    -ContentType "application/json"

Write-Host "`n2. Getting notifications for email..."
Invoke-RestMethod -Uri "$baseUrl/v1/notifications/$testEmail" -Method Get

Write-Host "`n3. Simulating movie release..."
Invoke-RestMethod -Uri "$baseUrl/test/simulate-release?movieName=Avengers:%20Secret%20Wars&location=New%20York" `
    -Method Post

Write-Host "`nTest completed!" 