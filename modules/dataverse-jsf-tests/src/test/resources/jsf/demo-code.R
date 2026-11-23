# Demo R analysis script
data <- read.csv("sample-data.csv")
summary(data)
cat("Mean score:", mean(data$score), "\n")
