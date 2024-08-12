document.addEventListener('DOMContentLoaded', (event) => {
    const bookingButton = document.querySelector('a.btn-primary');
    if (bookingButton) {
        bookingButton.addEventListener('click', (e) => {
            e.preventDefault();
            alert('Вы будете перенаправлены на запись в Telegram');
            window.open('https://t.me/crop_barber_bot', '_blank');
        });
    }
});
