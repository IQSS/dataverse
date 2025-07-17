function downloadRAHistoryCSV(url, filename) {
    fetch(url, {
        headers: {
            'Accept': 'text/csv'
        }
    })
    .then(response => response.blob())
    .then(blob => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    })
    .catch(error => console.error('Error downloading CSV:', error));
}