function downloadRAHistoryCSV(url, filename) {
    // Check if URL is null or undefined
    if (!url) {
        console.error('Error: Cannot download CSV - URL is null or undefined');
        // Display an error message to the user
        alert('Cannot download permissions history. The URL is not available.');
        return;
    }
    
    fetch(url, {
        headers: {
            'Accept': 'text/csv'
        }
    })
    .then(response => {
        // Check if response is ok (status in the range 200-299)
        if (!response.ok) {
            throw new Error('Network response was not ok: ' + response.status);
        }
        return response.blob();
    })
    .then(blob => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = filename || 'permissions_history.csv'; // Provide default filename if none provided
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    })
    .catch(error => {
        console.error('Error downloading CSV:', error);
        // Display a user-friendly error message
        alert('Failed to download permissions history. Please try again later.');
    });
}