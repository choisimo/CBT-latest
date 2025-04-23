#!/bin/bash

# Configuration - Customize these variables
WATCH_DIR="/path/to/your/documents"         # Directory to monitor
OUTPUT_DIR="/path/to/your/output"           # Directory for output files
LOG_DIR="/path/to/your/logs"                # Directory for logs
AI_API_ENDPOINT="http://your-ai-api.com"    # Replace with actual AI API endpoint
API_KEY="your-api-key"                      # Your API key
EMAIL_ADDRESS="user@example.com"            # User's email address
REVIEW_COUNT=3                              # Number of reviews to perform
DB_FILE="$LOG_DIR/processed_files.db"       # Database of processed files

# Install required packages if not already installed
sudo apt update
sudo apt install -y inotify-tools mailutils

# Create necessary directories
mkdir -p "$OUTPUT_DIR" "$LOG_DIR"
touch "$DB_FILE"

# Function to log messages
log() {
    local message="$1"
    local timestamp=$(date +"%Y-%m-%d %H:%M:%S")
    echo "[$timestamp] $message" >> "$LOG_DIR/file_monitor.log"
    echo "[$timestamp] $message"
}

# Function to check if a file has been processed
is_processed() {
    local file="$1"
    grep -q "^$file$" "$DB_FILE"
    return $?
}

# Function to mark a file as processed
mark_processed() {
    local file="$1"
    echo "$file" >> "$DB_FILE"
}

# Function to call AI API
call_ai_api() {
    local file="$1"
    
    # This is where you would implement the actual API call
    # For demonstration, we're using a placeholder
    log "Calling AI API to process $file"
    
    # Example API call (replace with your actual API implementation)
    curl -s -X POST \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $API_KEY" \
        -d "{\"text\": \"$(cat "$file" | sed 's/"/\\"/g')\"}" \
        "$AI_API_ENDPOINT"
}

# Function to process a file with the AI API
process_file() {
    local file="$1"
    local review_results=()
    
    log "Processing file: $file"
    
    # Check if file exists and is readable
    if [ ! -f "$file" ] || [ ! -r "$file" ]; then
        log "File does not exist or is not readable: $file"
        return 1
    fi
    
    # Check if file contains relevant keywords
    if ! grep -qiE "(instruction|exam|notification|schedule)" "$file"; then
        log "File does not contain relevant keywords: $file"
        mark_processed "$file"
        return 0
    fi
    
    # Perform multiple reviews
    for ((i=1; i<=$REVIEW_COUNT; i++)); do
        log "Review #$i"
        
        # Call AI API
        result=$(call_ai_api "$file")
        
        if [ $? -ne 0 ]; then
            log "API call failed for review #$i. Retrying in 30 seconds."
            sleep 30
            i=$((i-1))  # Retry this review
            continue
        fi
        
        review_results+=("$result")
    }
    
    # Create output file with current date
    current_date=$(date +"%Y-%m-%d")
    output_file="$OUTPUT_DIR/$current_date-instruction.txt"
    
    # Write results to output file
    {
        echo "========================================"
        echo "File: $file"
        echo "Date: $current_date"
        echo "========================================"
        echo "Results from multiple reviews:"
        echo ""
        
        for ((i=0; i<${#review_results[@]}; i++)); do
            echo "Review #$((i+1)):"
            echo "${review_results[$i]}"
            echo ""
        done
        
        echo "========================================"
        echo "Summary:"
        echo "The file has been processed $REVIEW_COUNT times."
        echo "Please review the results above to ensure all relevant information has been captured."
        echo "========================================"
    } > "$output_file"
    
    # Send email with results
    log "Sending results to $EMAIL_ADDRESS"
    mailx -s "Schedule/Instruction Processing Results - $current_date" "$EMAIL_ADDRESS" < "$output_file"
    
    # Mark file as processed
    mark_processed "$file"
    log "File processed successfully: $file"
    return 0
}

# Function to scan for unprocessed files
scan_unprocessed_files() {
    log "Scanning for unprocessed files in $WATCH_DIR"
    find "$WATCH_DIR" -type f -name "*.txt" | while read file; do
        if ! is_processed "$file"; then
            log "Found unprocessed file: $file"
            process_file "$file"
        fi
    done
}

# Function to handle signals
cleanup() {
    log "Stopping file monitor..."
    kill $(jobs -p) 2>/dev/null
    exit 0
}

# Set up signal handlers
trap cleanup SIGINT SIGTERM

# Initial scan for unprocessed files
scan_unprocessed_files

# Monitor directory for new files
log "Monitoring $WATCH_DIR for new text files..."
inotifywait -m -r -q -e close_write --format '%w%f' "$WATCH_DIR" | while read FILEPATH
do
    # Check if it's a text file and hasn't been processed
    if [[ "$FILEPATH" == *.txt ]] && ! is_processed "$FILEPATH"; then
        log "New text file detected: $FILEPATH"
        process_file "$FILEPATH"
    fi
done
