<?php

namespace App\Modules\Cli\Foundation;

class Controller
{
    protected array $viewVars = [];

    protected function setVar(string $name, mixed $value): void
    {
        $this->viewVars[$name] = $value;
    }
}