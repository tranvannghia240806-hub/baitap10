$(document).ready(function () {
    if ($('#profile').length) {
        $.ajax({
            type: 'GET',
            url: '/users/me',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            beforeSend: function (xhr) {
                if (localStorage.token) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + localStorage.token);
                }
            },
            success: function (data) {
                $('#profile').html(data.fullName);
                $('#images').attr('src', data.images);
                $('#emailLine').text(data.email);
            },
            error: function (e) {
                alert('Sorry, you are not logged in.');
                window.location.href = '/login';
            }
        });
    }

    $('#logout').click(function () {
        localStorage.clear();
        window.location.href = '/login';
    });

    $('#Login').click(function () {
        var email = document.getElementById('email').value;
        var password = document.getElementById('password').value;
        var basicInfo = JSON.stringify({
            email: email,
            password: password
        });
        $.ajax({
            type: 'POST',
            url: '/auth/login',
            dataType: 'json',
            contentType: 'application/json; charset=utf-8',
            data: basicInfo,
            success: function (data) {
                localStorage.token = data.token;
                window.location.href = '/user/profile';
            },
            error: function () {
                $('#loginError').text('Sai email hoặc mật khẩu.').removeClass('d-none');
            }
        });
    });

    $('#email, #password').keypress(function (e) {
        if (e.which === 13) { $('#Login').click(); }
    });
});